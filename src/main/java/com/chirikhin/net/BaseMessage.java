package com.chirikhin.net;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public abstract class BaseMessage {
    private static final Logger logger = Logger.getLogger(BaseMessage.class.getName());
    private final int id;

    public BaseMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    abstract void process(MySocketImpl mySocketImpl);


    protected byte[] getByteArrayWithTypeAndId(MessageType messageType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.asIntBuffer().put(getId());
        byte[] idByte = byteBuffer.array();
        return new byte[] {messageType.getValue(), idByte[0], idByte[1], idByte[2], idByte[3]};
    }

    abstract byte[] bytes();
}
