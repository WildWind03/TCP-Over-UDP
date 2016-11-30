package com.chirikhin.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

public class ServerMessageReceiver implements Runnable {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MessageReceiver.class.getName());
    private final BlockingQueue<ServerMessage> messages;
    private final DatagramSocket datagramSocket;

    private static final int SIZE_OF_DATAGRAM_PACKET = 2048;

    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[SIZE_OF_DATAGRAM_PACKET], SIZE_OF_DATAGRAM_PACKET);

    public ServerMessageReceiver(BlockingQueue<ServerMessage> messages, DatagramSocket datagramSocket) {
        this.messages = messages;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                datagramSocket.receive(datagramPacket);
                ServerMessage baseMessage = ServerMessageFactory.createMessage(datagramPacket);

                if (baseMessage.getBaseMessage() instanceof ByteMessage) {
                    System.out.println("New byte message was received!");
                }

                messages.put(baseMessage);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (InterruptedException e) {
            logger.error("The thread was interrupted");
        }
    }

}
