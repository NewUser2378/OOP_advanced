package info.kgeorgiy.ja.kupriyanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


// :NOTE: javadoc
public class HelloUDPNonblockingClient implements HelloClient {
    private final int TimeConst = 100;

    @Override
    public void run(String host, int port, String prefix, int threadsNumber, int numberOfRequests) {
        InetSocketAddress socket = new InetSocketAddress(host, port);

        try (Selector selector = Selector.open()) {
            for (int i = 1; i <= threadsNumber; i++) {
                DatagramChannel datagram = DatagramChannel.open();
                datagram.configureBlocking(false);
                datagram.connect(socket);
                datagram.register(selector, SelectionKey.OP_WRITE, new ClientContext(i, numberOfRequests, prefix));
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                selector.select(TimeConst);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isWritable()) {
                        writing(key) ;
                    } else if (key.isReadable()) {
                        reading(prefix, key);
                    }
                }

                long curTime = System.currentTimeMillis();
                for (SelectionKey key : selector.keys()) {
                    if (key.attachment() instanceof ClientContext context) {
                        if (curTime - context.lastSendTime > TimeConst) {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }

    private void writing(SelectionKey key) throws IOException {
        ClientContext context = (ClientContext) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();

        if (context.requestId > context.numberOfRequests) {
            channel.close();
            key.cancel();
            return;
        }
        String request = context.prefix + context.threadNum + "_" + context.requestId;
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8));
        channel.send(buffer, channel.getRemoteAddress());
        context.lastSendTime = System.currentTimeMillis();

        key.interestOps(SelectionKey.OP_READ);
    }

    private void reading(String prefix, SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        ClientContext context = (ClientContext) key.attachment();

        context.requestBuffer.clear();
        channel.receive(context.requestBuffer);
        context.requestBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(context.requestBuffer).toString();
        String expectedResponse = prefix + context.threadNum + "_" + context.requestId;
        if (response.contains(expectedResponse)) {
            validResponse(response, context, key);
        } else {
            invalidResponse(context, key);
        }
    }

    private void validResponse(String response, ClientContext context, SelectionKey key) {
        System.out.println(response);
        context.requestId++;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void invalidResponse(ClientContext context, SelectionKey key) {
        if (System.currentTimeMillis() - context.lastSendTime > TimeConst) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }


    private static class ClientContext {
        final int threadNum;
        final int numberOfRequests;
        final String prefix;
        int requestId = 1;
        ByteBuffer requestBuffer;
        long lastSendTime;

        ClientContext(int threadNum, int numberOfRequests, String prefix) {
            this.threadNum = threadNum;
            this.numberOfRequests = numberOfRequests;
            this.prefix = prefix;
            this.requestBuffer = ByteBuffer.allocate(1024);
        }
    }

    // :NOTE: unify with client

    public static void main(String[] args) {
        try {
            if (args.length < 5) {
                throw new IllegalArgumentException("not enough args");
            }
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threadsNumber = Integer.parseInt(args[3]);
            int numberOfRequests = Integer.parseInt(args[4]);
            new HelloUDPNonblockingClient().run(host, port, prefix, threadsNumber, numberOfRequests);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
