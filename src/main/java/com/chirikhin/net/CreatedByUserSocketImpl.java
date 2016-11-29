package com.chirikhin.net;

import com.chirikhin.cyclelist.CycleLinkedList;
import com.chirikhin.io.ListInputStream;
import com.chirikhin.io.ListOutputStream;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

class CreatedByUserSocketImpl extends MySocketImpl {
    private class MessageController implements Runnable {

        private final Logger logger = Logger.getLogger(MessageController.class.getName());
        private static final int SIZE_OF_HANDLED_MESSAGES_LIST = 1000;

        private final CycleLinkedList<BaseMessage> handledMessages = new CycleLinkedList<>(SIZE_OF_HANDLED_MESSAGES_LIST);

        @Override
        public void run() {
            try {
                BaseMessage baseMessage = receivedMessages.take();

                if (!isThisMessageAlreadyHandled(baseMessage)) {
                    baseMessage.process(CreatedByUserSocketImpl.this);
                    handledMessages.add(baseMessage);
                } else {
                    if (!(baseMessage instanceof ConfirmMessage) || ((ConfirmMessage) baseMessage).getConfirmMessageId() == ID_FOR_SALUTATION_MESSAGE) { //SolutationAnswer
                        messagesToSend.add(new ConfirmMessage(IDRegisterer.getInstance().getNext(), baseMessage.getId()));
                    }
                }

            } catch (InterruptedException e) {
                logger.info(e.getMessage());
            }
        }

        private boolean isThisMessageAlreadyHandled(BaseMessage baseMessage) {
            int id = baseMessage.getId();

            for (BaseMessage handledId : handledMessages) {
                if (id == handledId.getId()) {
                    return true;
                }
            }

            return false;
        }
    }

    private static final Logger logger = Logger.getLogger(CreatedByUserSocketImpl.class.getName());

    private static final int TCP_PORT = 12301;

    private final DatagramSocket datagramSocket;
    private final InetSocketAddress receiverInetSocketAddress;
    private final ListOutputStream listOutputStream;
    private final ListInputStream listInputStream;

    private final MessageReceiver messageReceiver;
    private final MessageSender messageSender;
    private final MessageResender messageResender;
    private final MessageController messageController;

    private final Thread messageReceiverThread;
    private final Thread messageSenderThread;
    private final Thread messageResenderThread;
    private final Thread messageControllerThread;

    private final BlockingQueue<BaseMessage> messagesToSend = new LinkedBlockingQueue<>();
    private final BlockingQueue<BaseMessage> receivedMessages = new LinkedTransferQueue<>();
    private final BlockingQueue<SentMessage> notConfirmedMessages = new LinkedBlockingQueue<>();

    private final BlockingQueue<byte[]> outputCollection = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> inputCollection = new LinkedBlockingQueue<>();

    CreatedByUserSocketImpl(String host, int port) throws SocketException, SocketTimeoutException, InterruptedException {
        datagramSocket = new DatagramSocket(TCP_PORT);
        receiverInetSocketAddress = new InetSocketAddress(host, port);
        listOutputStream = new ListOutputStream(outputCollection);
        listInputStream = new ListInputStream(inputCollection);

        messageReceiver = new MessageReceiver(receivedMessages, datagramSocket);
        messageSender = new MessageSender(messagesToSend, datagramSocket, receiverInetSocketAddress);
        messageResender = new MessageResender(messagesToSend, notConfirmedMessages);
        messageController = new MessageController();

        messageControllerThread = new Thread(messageController, "Message Controller Thread");
        messageReceiverThread = new Thread(messageReceiver, "Message Receiver Thread");
        messageSenderThread = new Thread(messageSender, "Message Sender Thread");
        messageResenderThread = new Thread(messageResender, "Message Resender Thread");

        messageControllerThread.start();
        messageReceiverThread.start();
        messageSenderThread.start();
        messageResenderThread.start();

        initHandshake();
    }

    private void initHandshake() throws InterruptedException, SocketTimeoutException {
        BaseMessage baseMessage = new SalutationMessage(IDRegisterer.getInstance().getNext());

        messagesToSend.add(baseMessage);
        notConfirmedMessages.add(new SentMessage(baseMessage, System.currentTimeMillis()));

        waitForConnection();
    }

    public int getPort() {
        return receiverInetSocketAddress.getPort();
    }

    public InputStream getInputStream() {
        return listInputStream;
    }

    public OutputStream getOutputStream() {
        return listOutputStream;
    }

    public void close() {
        CloseMessage closeMessage = new CloseMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(closeMessage);
        notConfirmedMessages.add(new SentMessage(closeMessage, System.currentTimeMillis()));
    }

    public void handleSalutationMessage(SalutationMessage salutationMessage) {
        haveConnectionSet();
    }

    public void handleByteMessage(ByteMessage byteMessage) {
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), byteMessage.getId());
        messagesToSend.add(response);
        inputCollection.add(byteMessage.bytes());
    }

    public void handleCloseMessage(CloseMessage closeMessage) {
        try {
            messageReceiver.close();
            listOutputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void handleConfirmMessage(ConfirmMessage confirmMessage) {
        Iterator<SentMessage> iterator = notConfirmedMessages.iterator();
        while (iterator.hasNext()) {
            SentMessage baseMessage = iterator.next();
            if (baseMessage.getBaseMessage().getId() == confirmMessage.getConfirmMessageId()) {
                iterator.remove();
            }
        }
    }
}
