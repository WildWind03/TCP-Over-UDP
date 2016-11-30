package com.chirikhin.net;

import org.apache.log4j.Logger;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerSocket {

    private class MessageController implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ServerMessage serverMessage = baseMessages.take();

                    BlockingQueue<BaseMessage> messagesForClient = clients.get(serverMessage.getInetAddress());
                    if (null == messagesForClient) {
                        connectionRequests.add(serverMessage);
                    } else {
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

        messageSender = new ServerMessageSender(messageToSend, datagramSocket);
        messageSenderThread = new Thread(messageSender, "Message Sender Thread");
        messageSenderThread.start();

        logger.info("Server Socket Started!");
    }

    public Socket accept() throws SocketException, SocketTimeoutException, InterruptedException {
        ServerMessage serverMessage = connectionRequests.take();
        logger.info("New connection request is accepting");

        if (serverMessage.getBaseMessage() instanceof SalutationMessage) {
            BlockingQueue<BaseMessage> baseMessages = new LinkedBlockingQueue<>();
            baseMessages.add(serverMessage.getBaseMessage());
            clients.put(serverMessage.getInetAddress(), baseMessages);

            return new Socket(new CreatedByServerSocketImpl(messageToSend, baseMessages, serverMessage.getInetAddress(), () -> clients.remove(serverMessage.getInetAddress())));
        } else {
            return accept();
        }
    }
}
