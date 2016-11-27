package com.chirikhin.net;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

public class MessageReceiver implements Runnable {
    private static final Logger logger = Logger.getLogger(MessageReceiver.class.getName());
    private final BlockingQueue<BaseMessage> messages;
    private final DatagramSocket datagramSocket;

    private static final int SIZE_OF_DATAGRAM_PACKET = 2048;

    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[SIZE_OF_DATAGRAM_PACKET], SIZE_OF_DATAGRAM_PACKET);

    public MessageReceiver(BlockingQueue<BaseMessage> messages, DatagramSocket datagramSocket) {
        this.messages = messages;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                datagramSocket.receive(datagramPacket);
                BaseMessage baseMessage = MessageFactory.createMessage(datagramPacket.getData());
                messages.put(baseMessage);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (InterruptedException e) {
            logger.error("The thread was interrupted");
        }
    }
}
