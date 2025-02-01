package info.kgeorgiy.ja.kupriyanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * use {@link HelloServer}.
 * class server to get tasks from client
 */
public class HelloUDPServer implements NewHelloServer {
    private final Map<Integer, DatagramSocket> sockets = new ConcurrentHashMap<>();
    private final Map<Integer, ExecutorService> workers = new ConcurrentHashMap<>();
    private final Map<Integer, ExecutorService> getters = new ConcurrentHashMap<>();

    /**
     * parsing arguments
     * @param args that we need to parse
     */
    private static Map<Integer, String> parsePorts(String[] args) {
        Map<Integer, String> ports = new ConcurrentHashMap<>();
        int i = 1;
        while  (i < args.length) {
            String[] parts = args[i].split(":");
            if (parts.length == 2) {
                int port = Integer.parseInt(parts[0]);
                String format = parts[1];
                ports.put(port, format);
            }
            i++;
        }
        return ports;
    }
    /**
     * method to use startServer with founded format
     * @param ports   for  mapping
     * @param threads number of threads
     */
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        for (Map.Entry<Integer, String> entry : ports.entrySet()) {
            String format = entry.getValue();
            int port = entry.getKey();
            startServer(port, format);
        }
    }
    /**
     * Start new Hello server
     * @param port for mapping

     * @param format int that got from port
     */
    private void startServer(int port, String format) {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            sockets.put(port, socket);

            ExecutorService getter = Executors.newSingleThreadExecutor();
            getters.put(port, getter);

            //исправил , сделал динамический пулл потоков
            ExecutorService worker = Executors.newCachedThreadPool();
            workers.put(port, worker);

            getter.submit(() -> {
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] recBuffer = new byte[socket.getReceiveBufferSize()];
                        DatagramPacket packet = new DatagramPacket(recBuffer, recBuffer.length);
                        socket.receive(packet);
                        worker.submit(() -> {
                            String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                            String response = format.replace("$", message);

                            byte[] resBuff = response.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket datagramPacket = new DatagramPacket(resBuff, resBuff.length, packet.getAddress(), packet.getPort());

                            try {
                                socket.send(datagramPacket);
                            } catch (IOException e) {
                                System.err.println("send error: " + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        if (!socket.isClosed()) {
                            System.err.println("socket error: " + e.getMessage());
                        }
                    }
                }
            });
        } catch (SocketException e) {
            System.err.println("creating error on " + port);
        }
    }
    /**
     * Stop socket and workers
     */
    @Override
    public void close() {
        sockets.forEach((port, socket) -> {
            socket.close();
            ExecutorService getter = getters.get(port);
            if (getter != null) {
                getter.shutdownNow();
            }
            ExecutorService worker = workers.get(port);
            if (worker != null) {
                worker.shutdownNow();
            }
        });
    }

    /**
     * Main function to use {@link HelloUDPServer}.
     *@param args array of argument
     */
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                throw new IllegalArgumentException("not enough arguments");
            }
            int threads = Integer.parseInt(Objects.requireNonNull(args[0]));
            Map<Integer, String> ports = parsePorts(args);

            try (HelloUDPServer server = new HelloUDPServer()) {
                server.start(threads, ports);
            }
        } catch (NumberFormatException e) {
            System.err.println("invalid number format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("unexpected error occurred: " + e.getMessage());
        }
    }
}