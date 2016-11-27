package com.chirikhin.net;

import com.chirikhin.io.ListInputStream;
import com.chirikhin.io.ListOutputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Logger;

class SocketImpl {
    private static final Logger logger = Logger.getLogger(SocketImpl.class.getName());
    private static final int TCP_PORT = 12301;
    private static final int TIME_TO_WAIT = 2000;

    private boolean isResponseReceived = false;

    private final DatagramSocket datagramSocket;
    private final InetSocketAddress receiverInetSocketAddress;
    private final ListOutputStream listOutputStream;
    private final ListInputStream listInputStream;

    private final MessageReceiver messageReceiver;
    private final MessageSender messageSender;
    private final MessageResender messageResender;

    private final BlockingQueue<BaseMessage> messagesToSend = new LinkedBlockingQueue<>();
    private final BlockingQueue<BaseMessage> receivedMessages = new LinkedTransferQueue<>();
    private final BlockingQueue<SentMessage> notConfirmedMessages = new LinkedBlockingQueue<>();

    SocketImpl(String host, int port, BlockingQueue<byte[]> outputCollection,
               BlockingQueue<byte[]> inputCollection) throws SocketException {
        datagramSocket = new DatagramSocket(TCP_PORT);
        receiverInetSocketAddress = new InetSocketAddress(host, port);
        listOutputStream = new ListOutputStream(outputCollection);
        listInputStream = new ListInputStream(inputCollection);

        messageReceiver = new MessageReceiver(receivedMessages, datagramSocket);
        messageSender = new MessageSender(messagesToSend, datagramSocket, receiverInetSocketAddress);
        messageResender = new MessageResender(messagesToSend, notConfirmedMessages);
    }

    void initAck() throws InterruptedException, SocketTimeoutException {
        BaseMessage baseMessage = new SalutationMessage(IDRegisterer.getInstance().getNext());
        baseMessage.attachFunction(confirmMessage -> {
            ConfirmMessage newConfirmMessage = new ConfirmMessage(IDRegisterer.getInstance().getNext(), confirmMessage.getId());
            messagesToSend.add(newConfirmMessage);
            isResponseReceived = true;
            baseMessage.notify();
        });

        messagesToSend.add(baseMessage);
        notConfirmedMessages.add(new SentMessage(baseMessage, System.currentTimeMillis()));

        long timeOfStart = System.currentTimeMillis();
        long timeToWait = TIME_TO_WAIT;

        while (timeToWait > 0) {
            baseMessage.wait(timeToWait);

            if (isResponseReceived) {
                return;
            } else {
                timeToWait = System.currentTimeMillis() - timeOfStart;
            }
        }

        throw new SocketTimeoutException("Can't set up a connection");
    }

    int getPort() {
        return receiverInetSocketAddress.getPort();
    }

    InputStream getInputStream() {
        return listInputStream;
    }

    OutputStream getOutputStream() {
        return listOutputStream;
    }

    void close() {

    }
}
