package info.kgeorgiy.ja.kupriyanov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import info.kgeorgiy.java.advanced.crawler.NewCrawler;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
public class WebCrawler implements NewCrawler {
    private final ExecutorService downloadExecutor;
    private final ExecutorService extractExecutor;
    private final Downloader downloader;

    public WebCrawler(Downloader downloader, int downloadThreads, int extractThreads, int maxConnections) {
        this.downloader = downloader;
        this.downloadExecutor = Executors.newFixedThreadPool(downloadThreads);
        this.extractExecutor = Executors.newFixedThreadPool(extractThreads);
    }

    @Override
    public Result download(String startUrl, int depth, Set<String> exclusions) {
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
        Queue<Future<List<String>>> extractionQueue = new ConcurrentLinkedQueue<>();
        List<String> downloadedPages = Collections.synchronizedList(new ArrayList<>());
        Map<String, IOException> failedDownloads = new ConcurrentHashMap<>();

        urlQueue.add(startUrl);
        visitedUrls.add(startUrl);
        int currentDepth = 0;
        while (currentDepth < depth) {
            List<Future<Document>> downloadFutures = new ArrayList<>();
            CompletionService<Document> completionService = new ExecutorCompletionService<>(downloadExecutor);

            while (!urlQueue.isEmpty()) {
                final String currentUrl = urlQueue.poll();
                if (exclusions.stream().anyMatch(currentUrl::contains)) {
                    continue;
                }
                Future<Document> document = completionService.submit(() -> {
                    try {
                        Document doc = downloader.download(currentUrl);
                        if (doc == null) {
                            return null;
                        } else {
                            downloadedPages.add(currentUrl);
                            return doc;
                        }
                    } catch (IOException e) {
                        failedDownloads.put(currentUrl, e);
                        return null;
                    }
                });
                downloadFutures.add(document);
            }
            for (Future<Document> future : downloadFutures) {
                try {
                    Document doc = future.get();
                    if (doc != null) {
                        extractionQueue.add(extractExecutor.submit(doc::extractLinks));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            for (Future<List<String>> extractedLinks : extractionQueue) {
                try {
                    List<String> links = extractedLinks.get();
                    for (String link : links) {
                        if (!visitedUrls.contains(link)) {
                            urlQueue.add(link);
                            visitedUrls.add(link);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            extractionQueue.clear();
            ++currentDepth;
        }

        downloadExecutor.shutdown();
        extractExecutor.shutdown();
        try {
            downloadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            extractExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Result(new ArrayList<>(downloadedPages), failedDownloads);
    }

    @Override
    public void close() {
        downloadExecutor.shutdown();
        extractExecutor.shutdown();
    }

    public static void main(String[] args) {
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Error: Null argument detected.");
            return;
        }
        try {
            String startUrl = Optional.ofNullable(args.length > 0 ? args[0] : null).orElse("");
            int depth = Optional.ofNullable(args.length > 1 ? args[1] : null).map(Integer::parseInt).orElse(1);
            int downloadThreads = Optional.ofNullable(args.length > 2 ? args[2] : null).map(Integer::parseInt).orElse(1);
            int extractThreads = Optional.ofNullable(args.length > 3 ? args[3] : null).map(Integer::parseInt).orElse(1);
            int maxConnections = Optional.ofNullable(args.length > 4 ? args[4] : null).map(Integer::parseInt).orElse(1);

            try (WebCrawler crawler = new WebCrawler(new CachingDownloader(maxConnections), downloadThreads, extractThreads, maxConnections)) {
                crawler.download(startUrl, depth, Collections.emptySet());
            }
        } catch (NumberFormatException e) {
            System.err.println("Error with parsing arg: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
    }
}