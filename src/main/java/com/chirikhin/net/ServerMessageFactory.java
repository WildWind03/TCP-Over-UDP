package com.chirikhin.net;

import org.apache.commons.lang3.ArrayUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class ServerMessageFactory {
    private static final Logger logger = Logger.getLogger(ServerMessageFactory.class.getName());

    public static ServerMessage createMessage(DatagramPacket datagramPacket) {

       BaseMessage baseMessage = MessageFactory.createMessage(ArrayUtils.subarray(datagramPacket.getData(), 0, datagramPacket.getLength()));
        return new ServerMessage(baseMessage, new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort()));

    }
}
