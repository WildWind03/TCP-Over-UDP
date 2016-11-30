package com.chirikhin.net;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class ServerMessageResender implements Runnable {
    private static final Logger logger = Logger.getLogger(MessageResender.class.getName());

    private static final long CHECK_PERIOD = 100;
    private static final long RESEND_TIME = 100;

    private final BlockingQueue<MessageToSend> messagesToSend;
    private final BlockingQueue<SentMessage> notConfirmedMessages;

    private final InetSocketAddress receiverInetSocketAddress;

    private Runnable runnable;

    public ServerMessageResender(BlockingQueue<MessageToSend> messagesToSend, BlockingQueue<SentMessage> notConfirmedMessages,
                                 InetSocketAddress receiverInetSocketAddress) {
        this.messagesToSend = messagesToSend;
        this.notConfirmedMessages = notConfirmedMessages;
        this.receiverInetSocketAddress = receiverInetSocketAddress;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                notConfirmedMessages
                        .stream()
                        .filter(sentMessage -> System.currentTimeMillis() - sentMessage.getTime() > RESEND_TIME)
                        .forEach(sentMessage -> {
                            messagesToSend.add(new MessageToSend(sentMessage.getBaseMessage(), receiverInetSocketAddress));
                            logger.info("New message was resent");
                            sentMessage.setTime(System.currentTimeMillis());
                        });

                Thread.sleep(CHECK_PERIOD);

                if (messagesToSend.isEmpty() && notConfirmedMessages.isEmpty() && null != runnable) {
                    runnable.run();
                    runnable = null;
                }
            }
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        }
    }

    public void setRunnableIfThereIsNothingToSend(Runnable runnable) {
        this.runnable = runnable;
    }
}
