package com.chirikhin.net;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

public class MessageSender implements Runnable {
    private static final Logger logger = Logger.getLogger(MessageSender.class.getName());

    private final BlockingQueue<BaseMessage> baseMessages;
    private final DatagramSocket datagramSocket;
    private final InetSocketAddress inetSocketAddress;

    public MessageSender(BlockingQueue<BaseMessage> messages, DatagramSocket datagramSocket, InetSocketAddress receiverAddress) {
        this.baseMessages = messages;
        this.datagramSocket = datagramSocket;
        this.inetSocketAddress = receiverAddress;
    }

    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()) {
                BaseMessage baseMessage = baseMessages.take();
                datagramSocket.send(new DatagramPacket(baseMessage.bytes(), baseMessage.bytes().length, inetSocketAddress));
            }
        } catch (InterruptedException e) {
            logger.info ("Thread was interrupted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
