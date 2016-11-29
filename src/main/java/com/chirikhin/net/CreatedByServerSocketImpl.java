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
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
        private long timeOfLastPoll;
        private Runnable runnableWhenThereIsNothingToHandle;

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    BaseMessage baseMessage = receivedMessages.poll(TIMEOUT_FOR_POLL_FROM_QUEUE, TimeUnit.MILLISECONDS);

                    if (null == baseMessage || !messageFilter.test(baseMessage)) {
                        continue;
                    }

                    if (isGoingToClose) {
                        if (System.currentTimeMillis() - timeOfLastPoll > MAX_TIME_BETWEEN_MESSAGES_WHEN_CLOSE) {
                            runnableWhenThereIsNothingToHandle.run();
                            return;
                        } else {
                            timeOfLastPoll = System.currentTimeMillis();
                        }
                    }

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
    private final BlockingQueue<SentMessage> notConfirmedMessages;

    private final BlockingQueue<byte[]> outputCollection = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> inputCollection = new LinkedBlockingQueue<>();

    private final ListOutputStream listOutputStream = new ListOutputStream(outputCollection);
    private final ListInputStream listInputStream = new ListInputStream(inputCollection);

    private boolean isReadyToClose = false;
    private boolean isClosed = false;

    CreatedByServerSocketImpl(BlockingQueue<MessageToSend> messagesToSend, BlockingQueue<BaseMessage> messagesToRead,
                              BlockingQueue<SentMessage> notConfirmedMessages, InetSocketAddress receiverInetSocketAddress) throws SocketTimeoutException, InterruptedException {
        this.messagesToSend = messagesToSend;
        this.receivedMessages = messagesToRead;
        this.notConfirmedMessages = notConfirmedMessages;
        this.receiverInetSocketAddress = receiverInetSocketAddress;

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
        listOutputStream.close();
        listInputStream.close();

        Object lock = new Object();

        messageResender.setRunnableIfThereIsNothingToSend(() -> {
            isReadyToClose = true;
            lock.notify();
        });

        messageController.setMessageFilter(baseMessage -> !(baseMessage instanceof ByteMessage));

        try {
            while (!isReadyToClose) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        CloseMessage closeMessage = new CloseMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(new MessageToSend(closeMessage, receiverInetSocketAddress));
        notConfirmedMessages.add(new SentMessage(closeMessage, System.currentTimeMillis()));

        try {
            messageController.setRunnableIfThereIsNothingToHandle(new Runnable() {
                @Override
                public void run() {
                    isClosed = true;
                    lock.notify();
                }
            });

            while (!isClosed) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public void handleSalutationMessage(SalutationMessage salutationMessage) {
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), salutationMessage.getId());
        messagesToSend.add(new MessageToSend(response, receiverInetSocketAddress));

        BaseMessage baseMessage = new SalutationMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(new MessageToSend(baseMessage, receiverInetSocketAddress));
        notConfirmedMessages.add(new SentMessage(baseMessage, System.currentTimeMillis()));
    }

    public void handleByteMessage(ByteMessage byteMessage) {
        BaseMessage response = new ConfirmMessage(IDRegisterer.getInstance().getNext(), byteMessage.getId());
        messagesToSend.add(new MessageToSend(response, receiverInetSocketAddress));
        inputCollection.add(byteMessage.bytes());

    }

    public void handleCloseMessage(CloseMessage closeMessage) {
        BaseMessage baseMessage = new ConfirmMessage(IDRegisterer.getInstance().getNext(), closeMessage.getId());
        messagesToSend.add(new MessageToSend(baseMessage, receiverInetSocketAddress));

        CloseMessage myCloseMessage = new CloseMessage(IDRegisterer.getInstance().getNext());
        messagesToSend.add(new MessageToSend(myCloseMessage, receiverInetSocketAddress));
        notConfirmedMessages.add(new SentMessage(myCloseMessage, System.currentTimeMillis()));
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