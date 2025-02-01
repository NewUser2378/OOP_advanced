package info.kgeorgiy.ja.kupriyanov.hello;


import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements NewHelloServer {
    private final Map<Integer, DatagramChannel> channels = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Selector selector;

    /**
     * Method to use startServer with found format
     * @param ports   for mapping
     * @param threads number of threads
     */
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        try {
            selector = Selector.open();

            for (Map.Entry<Integer, String> entry : ports.entrySet()) {
                String format = entry.getValue();
                int port = entry.getKey();
                startServer(port, format);
            }

            CompletableFuture.runAsync(this::runSelector, executor);
        } catch (IOException e) {
            System.err.println("Selector error: " + e.getMessage());
        }
    }
    /**
     * Method to run Selector
     */
    private void runSelector() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                selector.select();

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        reading(key);
                    }
                }
            } catch (IOException e) {
                System.err.println("Selector error: " + e.getMessage());
            }
        }
    }

    /**
     * Start new Hello server
     * @param port for mapping
     * @param format int that got from port
     */
    private void startServer(int port, String format) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ, format);
            channels.put(port, channel);
        } catch (IOException e) {
            System.err.println("Error creating server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Method for reading and getting adress
     */
    private void reading(SelectionKey key) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        String format = (String) key.attachment();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            SocketAddress socket = channel.receive(buffer);
            if (socket != null) {
                buffer.flip();
                String message = StandardCharsets.UTF_8.decode(buffer).toString();
                String response = format.replace("$", message);

                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
                channel.send(responseBuffer, socket);
            }
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }

    /**
     * Stop channels
     */
    @Override
    public void close() {
        // :NOTE: close instead shutdown
        executor.shutdownNow();
        channels.forEach((port, channel) -> {
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Error closing channel on port " + port + ": " + e.getMessage());
            }
        });
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        }
    }

    /**
     * Main function to use {@link HelloUDPNonblockingServer}.
     * @param args array of argument
     */

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                throw new IllegalArgumentException("Not enough args");
            }
            int threads = Integer.parseInt(Objects.requireNonNull(args[0]));
            Map<Integer, String> ports = new ConcurrentHashMap<>();
            for (int i = 1; i < args.length; i++) {
                String[] parts = args[i].split(":");
                if (parts.length == 2) {
                    int port = Integer.parseInt(parts[0]);
                    String format = parts[1];
                    ports.put(port, format);
                }
            }

            try (HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
                server.start(threads, ports);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
        }
    }
}
