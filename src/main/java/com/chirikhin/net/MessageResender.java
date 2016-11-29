package com.chirikhin.net;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class MessageResender implements Runnable {
    private static final Logger logger = Logger.getLogger(MessageResender.class.getName());

    private static final long CHECK_PERIOD = 500;
    private static final long RESEND_TIME = 500;

    private final BlockingQueue<BaseMessage> messagesToSend;
    private final BlockingQueue<SentMessage> notConfirmedMessages;

    public MessageResender(BlockingQueue<BaseMessage> messagesToSend, BlockingQueue<SentMessage> notConfirmedMessages) {
        this.messagesToSend = messagesToSend;
        this.notConfirmedMessages = notConfirmedMessages;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                notConfirmedMessages
                        .stream()
                        .filter(sentMessage -> System.currentTimeMillis() - sentMessage.getTime() > RESEND_TIME)
                        .forEach(sentMessage -> {
                    messagesToSend.add(sentMessage.getBaseMessage());
                    sentMessage.setTime(System.currentTimeMillis());
                });

                Thread.sleep(CHECK_PERIOD);
            }
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        }
    }
}
