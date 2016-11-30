package com.chirikhin.net;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

public class ServerMessageSender implements Runnable {
    private static final Logger logger = org.apache.log4j.Logger.getLogger(ServerMessageSender.class.getName());

    private final BlockingQueue<MessageToSend> baseMessages;
    private final DatagramSocket datagramSocket;

    public ServerMessageSender(BlockingQueue<MessageToSend> messages, DatagramSocket datagramSocket) {
        this.baseMessages = messages;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()) {
                MessageToSend baseMessage = baseMessages.take();

                logger.info("Sending new message!");
                datagramSocket.send(new DatagramPacket(baseMessage.getSentMessage().bytes(), baseMessage.getSentMessage().bytes().length, baseMessage.getInetSocketAddress()));
            }
        } catch (InterruptedException e) {
            logger.info ("Thread was interrupted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
