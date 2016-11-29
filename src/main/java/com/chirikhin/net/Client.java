package com.chirikhin.net;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private final BlockingQueue<BaseMessage> messagesToSend;
    private final BlockingQueue<BaseMessage> receivedMessages;

    public Client(BlockingQueue<BaseMessage> messagesToSend, BlockingQueue<BaseMessage> receivedMessages) {
        this.messagesToSend = messagesToSend;
        this.receivedMessages = receivedMessages;
    }

    public void passMessage(BaseMessage baseMessage) {
        receivedMessages.add(baseMessage);
    }

    public BaseMessage getMessage

}
