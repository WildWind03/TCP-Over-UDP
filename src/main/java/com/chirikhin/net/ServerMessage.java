package com.chirikhin.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class ServerMessage {
    private static final Logger logger = Logger.getLogger(ServerMessage.class.getName());

    private final BaseMessage baseMessage;
    private final InetSocketAddress inetAddress;

    public ServerMessage(BaseMessage baseMessage, InetSocketAddress inetAddress) {
        this.baseMessage = baseMessage;
        this.inetAddress = inetAddress;
    }

    public BaseMessage getBaseMessage() {
        return baseMessage;
    }

    public InetSocketAddress getInetAddress() {
        return inetAddress;
    }
}
