package info.kgeorgiy.ja.kupriyanov.hello;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 * use {@link HelloClient }
 * class client working on UDP
 */

public class HelloUDPClient implements HelloClient {

    private final int timeConst=100;
    /**
     * base method to send requests and get responses from server
     *
     * @param host             hostname of server to send requests
     * @param port             port number of the server
     * @param prefix           prefix to add to the path of the request URL
     * @param threadsNumber    number of concurrent threads to use
     * @param numberOfRequests total number of requests
     */
    @Override
    public void run(String host, int port, String prefix, int threadsNumber, int numberOfRequests) {
        InetSocketAddress address = new InetSocketAddress(host, port);

        ExecutorService workers = Executors.newFixedThreadPool(threadsNumber);
        int i = 1;
        while ( i <= threadsNumber) {
            final int threadId = i;
            workers.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    int size = socket.getReceiveBufferSize();
                    byte[] resBuffer = new byte[size];
                    DatagramPacket response = new DatagramPacket(resBuffer, resBuffer.length);
                    for (int requestId = 1; requestId <= numberOfRequests; ++requestId) {
                        String request = prefix + threadId + "_" + requestId;
                        byte[] requestBuffer = request.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(requestBuffer, requestBuffer.length, address);
                        // исправил, добавил константу
                        socket.setSoTimeout(timeConst);
                        boolean requestSent = false;
                        while (!requestSent) {
                            try {
                                socket.send(packet);
                                socket.receive(response);
                                String result = new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                                if (result.contains(request)) {
                                    System.out.println(result);
                                    requestSent = true;
                                }
                            } catch (SocketTimeoutException e) {
                                System.err.println("timeout: " + e.getMessage());
                            } catch (IOException e) {
                                System.err.println("sending error: " + e.getMessage());
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.err.println("problems with socket" + e.getMessage());
                }
            });
            i++;
        }
        //исправил закрытие
        workers.close();
        workers.shutdown();
        try {
            if (!workers.awaitTermination((long) threadsNumber * numberOfRequests, TimeUnit.MINUTES)) {
                System.err.println("Thread pool did not terminate within the specified time limit.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread pool was interrupted while awaiting termination: " + e.getMessage());
        }
    }

    /**
     * Main function for running with args.
     *
     * @param args array of argument
     */
    public static void main(String[] args) {
        try {
            if (args.length < 5) {
                throw new IllegalArgumentException("not enough arguments");
            }
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threadsNumber = Integer.parseInt(args[3]);
            int numberOfRequests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(host, port, prefix, threadsNumber, numberOfRequests);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
