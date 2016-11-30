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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;

class CreatedByUserSocketImpl extends MySocketImpl {
    private class MessageController implements Runnable {

        private final Logger logger = Logger.getLogger(MessageController.class.getName());
        private static final int SIZE_OF_HANDLED_MESSAGES_LIST = 1000;

        private final CycleLinkedList<BaseMessage> handledMessages = new CycleLinkedList<>(SIZE_OF_HANDLED_MESSAGES_LIST);

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    BaseMessage baseMessage = receivedMessages.take();

                    if (baseMessage instanceof SalutationMessage) {
                        logger.info("Controller is handling salutation message");
                    }

                    if (!isThisMessageAlreadyHandled(baseMessage)) {
                        baseMessage.process(CreatedByUserSocketImpl.this);
                        handledMessages.add(baseMessage);
                    } else {
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

    private final BlockingQueue<byte[]> inputCollection = new LinkedBlockingQueue<>();

    private final Map<Integer, byte[]> cachedBytes = new HashMap<>();
    private int expectedPart = 0;

    private boolean isClosed = false;
    private boolean isReadyToClose = false;

    private int partToSend = 0;


    CreatedByUserSocketImpl(String host, int port) throws SocketException, SocketTimeoutException, InterruptedException {
        datagramSocket = new DatagramSocket(TCP_PORT);
        receiverInetSocketAddress = new InetSocketAddress(host, port);
        listOutputStream = new ListOutputStream(bytes -> {
            ByteMessage byteMessage = new ByteMessage(IDRegisterer.getInstance().getNext(), partToSend++, bytes);
            messagesToSend.add(byteMessage);
            logger.info("New message was added to messages that are going to be sent");
            notConfirmedMessages.add(new SentMessage(byteMessage, System.currentTimeMillis()));
        });
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
        System.out.println("Start closing");
        listOutputStream.close();
        listInputStream.close();
        messageReceiver.setMessageFilter(baseMessage -> !(baseMessage instanceof ByteMessage));

        Object lock = new Object();

        messageResender.setRunnableIfThereIsNothingToSend(() -> {
            synchronized (lock) {
                isReadyToClose = true;
                lock.notify();
            }
        });

        System.out.println("Block until all the messages will be handled");

        synchronized (lock) {
            try {
                while (!isReadyToClose) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        System.out.println("All the messages are handled");

        CloseMessage closeMessage = new CloseMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(closeMessage);
        notConfirmedMessages.add(new SentMessage(closeMessage, System.currentTimeMillis()));

        System.out.println("Block until another socket will stop sending messages");

        try {
            messageReceiver.close(() -> {
                synchronized (lock) {
                    isClosed = true;
                    lock.notify();
                }
            });

            synchronized (lock) {
                while (!isClosed) {
                    lock.wait();
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }

        System.out.println("Stopped");
    }

    public void handleSalutationMessage(SalutationMessage salutationMessage) {
        logger.info("Handling salutation message");
        haveConnectionSet();
    }

    public void handleByteMessage(ByteMessage byteMessage) {
        logger.info("Handling byte message");
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), byteMessage.getId());
        messagesToSend.add(response);


        if (expectedPart == byteMessage.getPart()) {
            expectedPart++;
            inputCollection.add(byteMessage.getContentedByted());

            while (true) {
                byte[] cachedMessage = cachedBytes.get(expectedPart);

                if (null != cachedMessage) {
                    inputCollection.add(cachedMessage);
                    expectedPart++;
                } else {
                    break;
                }
            }
        }
    }

    public void handleCloseMessage(CloseMessage closeMessage) {
        messagesToSend.clear();
        notConfirmedMessages.clear();

        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), closeMessage.getId());
        messagesToSend.add(response);

        //messageSenderThread.interrupt();
        //messageReceiverThread.interrupt();

        logger.info("Handling close message");
        try {
            messageReceiver.close();
            listOutputStream.close();
            listInputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void handleConfirmMessage(ConfirmMessage confirmMessage) {
        logger.info("Handling confirm messages!");

        Iterator<SentMessage> iterator = notConfirmedMessages.iterator();
        while (iterator.hasNext()) {
            SentMessage baseMessage = iterator.next();
            if (baseMessage.getBaseMessage().getId() == confirmMessage.getConfirmMessageId()) {
                iterator.remove();
            }
        }
    }
}
