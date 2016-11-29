package com.chirikhin.net;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class ConfirmMessage extends BaseMessage {
    private static final Logger logger = Logger.getLogger(ConfirmMessage.class.getName());
    private final int confirmMessageId;

    public ConfirmMessage(int id, int confirmMessageId) {
        super(id);
        this.confirmMessageId = confirmMessageId;
    }

    @Override
    void process(MySocketImpl mySocketImpl) {
        mySocketImpl.handleConfirmMessage(this);
    }

    @Override
    byte[] bytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.asIntBuffer().put(confirmMessageId);
        byte[] idConfirmMessage = byteBuffer.array();

        return ArrayUtils.addAll(getByteArrayWithTypeAndId(MessageType.CONFIRM), idConfirmMessage);
    }

    public int getConfirmMessageId() {
        return confirmMessageId;
    }
}
