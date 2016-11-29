package com.chirikhin.net;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class MessageToSend {
    private static final Logger logger = Logger.getLogger(MessageToSend.class.getName());
    private final BaseMessage baseMessage;
    private final InetSocketAddress inetSocketAddress;

    public MessageToSend(BaseMessage baseMessage, InetSocketAddress inetSocketAddress) {
        this.baseMessage = baseMessage;
        this.inetSocketAddress = inetSocketAddress;
    }

    public BaseMessage getSentMessage() {
        return baseMessage;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }
}
