package com.chirikhin.net;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class ServerSocket {
    private class MessageController implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ServerMessage serverMessage = baseMessages.take();
                    InetSocketAddress inetAddress = serverMessage.getInetAddress();
                    if (null == inetAddress) {
                        connectionRequests.add(serverMessage);
                    } else {
                        BlockingQueue<BaseMessage> messagesForClient = clients.get(inetAddress);
                        messagesForClient.add(serverMessage.getBaseMessage());
                    }
                }
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
            }
        }
    }


    private static final Logger logger = Logger.getLogger(ServerSocket.class.getName());

    private final DatagramSocket datagramSocket;

    private Map<InetSocketAddress, BlockingQueue<BaseMessage>> clients = new HashMap<>();

    private final ServerMessageReceiver messageReceiver;
    private final Thread messageReceiverThread;

    private final MessageController messageController;
    private final Thread messageControllerThread;

    private final ServerMessageSender messageSender;
    private final Thread messageSenderThread;

    private final BlockingQueue<ServerMessage> baseMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<ServerMessage> connectionRequests = new LinkedBlockingQueue<>();
    private final BlockingQueue<MessageToSend> messageToSend = new LinkedBlockingQueue<>();

    public ServerSocket(int port) throws SocketException {
        this.datagramSocket = new DatagramSocket(port);
        messageReceiver = new ServerMessageReceiver(baseMessages, datagramSocket);
        messageReceiverThread = new Thread(messageReceiver, "Message Receiver Thread");
        messageReceiverThread.start();

        messageController = new MessageController();
        messageControllerThread = new Thread(messageController, "Message Controller Thread");
        messageControllerThread.start();
    }

    public Socket accept() throws SocketException, SocketTimeoutException, InterruptedException {
        ServerMessage serverMessage = connectionRequests.take();

        if (serverMessage.getBaseMessage() instanceof SalutationMessage) {
            BlockingQueue<BaseMessage> baseMessages = new LinkedBlockingQueue<>();
            clients.put(serverMessage.getInetAddress(), baseMessages);

            return new Socket(new CreatedByServerSocketImpl(messageToSend, ));
        } else {
            return accept();
        }
    }
}
