package com.chirikhin.net;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

public class MessageReceiver implements Runnable, Closeable {
    private static final Logger logger = Logger.getLogger(MessageReceiver.class.getName());
    private static final int SOCKET_READ_TIMEOUT = 1000;
    private static final int TIME_THAT_MEANS_LOSE_CONNECTION = 2000;

    private final BlockingQueue<BaseMessage> messages;
    private final DatagramSocket datagramSocket;

    private static final int SIZE_OF_DATAGRAM_PACKET = 2048;
    private boolean isClosed = false;
    private long timeOfLastReceivedMessage;
    private Runnable runnable;
    private Predicate<BaseMessage> messageFilter = BaseMessage -> true;

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
                       if (null != runnable) {
                           runnable.run();
                       }
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

                System.out.println("Received data: " + datagramPacket.getLength());


                BaseMessage baseMessage = MessageFactory.createMessage(ArrayUtils.subarray(datagramPacket.getData(), 0, datagramPacket.getLength()));

                if (baseMessage instanceof SalutationMessage) {
                    logger.info("Salutation Message was received!");
                }

                if (messageFilter.test(baseMessage)) {
                    messages.put(baseMessage);
                    logger.info("Salutation Message was put into queue");
                }
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

    public void close(Runnable runnable) throws IOException {
        close();
        this.runnable = runnable;
    }

    public void setMessageFilter(Predicate<BaseMessage> filter) {
        this.messageFilter = filter;
    }
}
