package com.chirikhin.net;

import java.util.logging.Logger;

public class SalutationMessage extends BaseMessage {
    private static final Logger logger = Logger.getLogger(SalutationMessage.class.getName());

    public SalutationMessage(int id) {
        super(id);
    }

    @Override
    void process(MySocketImpl mySocketImpl) {
        mySocketImpl.handleSalutationMessage(this);
    }

    @Override
    public byte[] bytes() {
        return getByteArrayWithTypeAndId(MessageType.SALUTATION);
    }
}
