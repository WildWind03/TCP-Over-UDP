package com.chirikhin.net;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class MessageReceiver extends AbstractMessageReceiver implements Closeable {
    private static final Logger logger = Logger.getLogger(MessageReceiver.class.getName());
    private static final int SOCKET_READ_TIMEOUT = 1000;
    private static final int TIME_THAT_MEANS_LOSE_CONNECTION = 2000;

    private final BlockingQueue<BaseMessage> messages;
    private final DatagramSocket datagramSocket;

    private static final int SIZE_OF_DATAGRAM_PACKET = 2048;
    private boolean isClosed = false;
    private long timeOfLastReceivedMessage;

    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[SIZE_OF_DATAGRAM_PACKET], SIZE_OF_DATAGRAM_PACKET);

    public MessageReceiver(BlockingQueue<BaseMessage> messages, DatagramSocket datagramSocket) throws SocketException {
        this.messages = messages;
        this.datagramSocket = datagramSocket;
        datagramSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (isClosed) {
                    if (System.currentTimeMillis() - timeOfLastReceivedMessage > TIME_THAT_MEANS_LOSE_CONNECTION) {
                        return;
                    }
                }

                try {
                    datagramSocket.receive(datagramPacket);
                    if (isClosed) {
                        timeOfLastReceivedMessage = System.currentTimeMillis();
                    }
                } catch (SocketTimeoutException e) {
                    continue;
                }


                BaseMessage baseMessage = MessageFactory.createMessage(datagramPacket.getData());
                messages.put(baseMessage);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (InterruptedException e) {
            logger.error("The thread was interrupted");
        }
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
        timeOfLastReceivedMessage = System.currentTimeMillis();
    }
}
