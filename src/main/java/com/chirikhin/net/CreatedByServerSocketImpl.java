package com.chirikhin.net;

import com.chirikhin.cyclelist.CycleLinkedList;
import com.chirikhin.io.ListInputStream;
import com.chirikhin.io.ListOutputStream;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

class CreatedByServerSocketImpl extends MySocketImpl {
    private class MessageController implements Runnable, Closeable {

        private final Logger logger = Logger.getLogger(MessageController.class.getName());
        private static final int SIZE_OF_HANDLED_MESSAGES_LIST = 1000;
        private static final int TIMEOUT_FOR_POLL_FROM_QUEUE = 1000;
        private static final int MAX_TIME_BETWEEN_MESSAGES_WHEN_CLOSE = 1000;

        private Predicate<BaseMessage> messageFilter = baseMessage -> true;

        private final CycleLinkedList<BaseMessage> handledMessages = new CycleLinkedList<>(SIZE_OF_HANDLED_MESSAGES_LIST);

        private boolean isGoingToClose = false;
        private long timeOfLastPoll = -1;
        private Runnable runnableWhenThereIsNothingToHandle;

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    BaseMessage baseMessage = receivedMessages.poll(TIMEOUT_FOR_POLL_FROM_QUEUE, TimeUnit.MILLISECONDS);

                    if (isGoingToClose) {
                        if (timeOfLastPoll < 0) {
                            timeOfLastPoll = System.currentTimeMillis();
                        }

                        if (System.currentTimeMillis() - timeOfLastPoll > MAX_TIME_BETWEEN_MESSAGES_WHEN_CLOSE) {
                            runnableWhenThereIsNothingToHandle.run();
                            return;
                        } else {
                            if (null != baseMessage) {
                                timeOfLastPoll = System.currentTimeMillis();
                            }
                        }
                    }

                    if (null == baseMessage || !messageFilter.test(baseMessage)) {
                        continue;
                    }

                    System.out.println("Got new not null message!");


                    if (!isConnectionSet() && !(baseMessage instanceof SalutationMessage)) {
                        haveConnectionSet();
                    }

                    if (!isThisMessageAlreadyHandled(baseMessage)) {
                        baseMessage.process(CreatedByServerSocketImpl.this);
                        handledMessages.add(baseMessage);
                    } else {
                        messagesToSend.add(new MessageToSend(new ConfirmMessage(IDRegisterer.getInstance().getNext(), baseMessage.getId()), receiverInetSocketAddress));
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

        public void setMessageFilter(Predicate<BaseMessage> messageFilter) {
            this.messageFilter = messageFilter;
        }

        public void setRunnableIfThereIsNothingToHandle(Runnable runnableIfThereIsNothingToHandle) {
            this.runnableWhenThereIsNothingToHandle = runnableIfThereIsNothingToHandle;
        }

        @Override
        public void close() {
            isGoingToClose = true;
        }
    }

    private static final Logger logger = Logger.getLogger(CreatedByUserSocketImpl.class.getName());

    private final InetSocketAddress receiverInetSocketAddress;

    private final MessageController messageController;
    private final Thread messageControllerThread;

    private final ServerMessageResender messageResender;
    private final Thread messageResenderThread;

    private final BlockingQueue<MessageToSend> messagesToSend;
    private final BlockingQueue<BaseMessage> receivedMessages;
    private final BlockingQueue<SentMessage> notConfirmedMessages = new LinkedBlockingQueue<>();

    private final BlockingQueue<byte[]> inputCollection = new LinkedBlockingQueue<>();

    private final ListOutputStream listOutputStream;
    private final ListInputStream listInputStream = new ListInputStream(inputCollection);

    private boolean isReadyToClose = false;
    private boolean isClosed = false;

    private int partToSend = 0;

    private final Map<Integer, byte[]> cachedBytes = new HashMap<>();
    private int expectedPart = 0;
    private Runnable commandOnClose;

    CreatedByServerSocketImpl(BlockingQueue<MessageToSend> messagesToSend, BlockingQueue<BaseMessage> messagesToRead,
                              InetSocketAddress receiverInetSocketAddress, Runnable commandOnClose) throws SocketTimeoutException, InterruptedException {
        this.messagesToSend = messagesToSend;
        this.receivedMessages = messagesToRead;
        this.receiverInetSocketAddress = receiverInetSocketAddress;
        this.commandOnClose = commandOnClose;

        listOutputStream = new ListOutputStream(bytes -> {
            ByteMessage byteMessage = new ByteMessage(IDRegisterer.getInstance().getNext(), partToSend++, bytes);
            messagesToSend.add(new MessageToSend(byteMessage, receiverInetSocketAddress));
            notConfirmedMessages.add(new SentMessage(byteMessage, System.currentTimeMillis()));
        });

        messageController = new MessageController();
        messageControllerThread = new Thread(messageController, "Message Controller Thread");
        messageControllerThread.start();

        messageResender = new ServerMessageResender(messagesToSend, notConfirmedMessages, receiverInetSocketAddress);
        messageResenderThread = new Thread(messageResender, "Message Resender Thread");
        messageResenderThread.start();

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

        Object lock = new Object();

        messageResender.setRunnableIfThereIsNothingToSend(() -> {
            synchronized (lock) {
                isReadyToClose = true;
                lock.notify();
            }
        });

        messageController.setMessageFilter(baseMessage -> !(baseMessage instanceof ByteMessage));

        System.out.println("Blocking untill all the messages will be delivered");

        synchronized (lock) {

            try {
                while (!isReadyToClose) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        System.out.println("All the messages have been sent!");

        CloseMessage closeMessage = new CloseMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(new MessageToSend(closeMessage, receiverInetSocketAddress));
        notConfirmedMessages.add(new SentMessage(closeMessage, System.currentTimeMillis()));

        messageController.close();

        try {
            messageController.setRunnableIfThereIsNothingToHandle(() -> {
                synchronized (lock) {
                    isClosed = true;
                    lock.notify();
                }
            });

            System.out.println("Block until all the messages will stop coming");

            synchronized (lock) {
                while (!isClosed) {
                    lock.wait();
                }
            }
            } catch(InterruptedException e){
                logger.error(e.getMessage());
            }

            commandOnClose.run();

        messagesToSend.clear();
        notConfirmedMessages.clear();
        receivedMessages.clear();
        listInputStream.close();
        listOutputStream.close();

        messageResenderThread.interrupt();
        messageControllerThread.interrupt();

        System.out.println("Stopped!");
    }

    public void handleSalutationMessage(SalutationMessage salutationMessage) {
        logger.info("Handling salutation message");
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), salutationMessage.getId());
        messagesToSend.add(new MessageToSend(response, receiverInetSocketAddress));

        BaseMessage baseMessage = new SalutationMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(new MessageToSend(baseMessage, receiverInetSocketAddress));
        notConfirmedMessages.add(new SentMessage(baseMessage, System.currentTimeMillis()));
    }

    public void handleByteMessage(ByteMessage byteMessage) {
        logger.info("Handling byte message");
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), byteMessage.getId());
        messagesToSend.add(new MessageToSend(response, receiverInetSocketAddress));

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
        } else {
            cachedBytes.put(byteMessage.getPart(), byteMessage.getContentedByted());
        }
    }

    public void handleCloseMessage(CloseMessage closeMessage) {
        new Thread(() -> {
            messagesToSend.clear();
            notConfirmedMessages.clear();
            receivedMessages.clear();

            messageResenderThread.interrupt();
            messageControllerThread.interrupt();

            logger.info("Handling close message");
            listInputStream.close();
            listOutputStream.close();
        });

        commandOnClose.run();
    }

    public void handleConfirmMessage(ConfirmMessage confirmMessage) {
        logger.info("Handling confirm message");
        Iterator<SentMessage> iterator = notConfirmedMessages.iterator();
        while (iterator.hasNext()) {
            SentMessage baseMessage = iterator.next();
            if (baseMessage.getBaseMessage().getId() == confirmMessage.getConfirmMessageId()) {
                iterator.remove();
            }
        }
    }
}