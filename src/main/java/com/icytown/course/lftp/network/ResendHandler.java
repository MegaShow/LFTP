package com.icytown.course.lftp.network;

import java.io.IOException;

public class ResendHandler {
    private IOException resendException = null;
    private int duplicateAck = 0;
    public void collectionResendException(IOException e) {
        resendException = e;
    }
    public IOException getResendException() {
        return resendException;
    }
    public void resetAck() {
        duplicateAck = 0;
    }
    public void addAck() {
        duplicateAck++;
    }
    public int getDuplicateAck() {
        return duplicateAck;
    }
}
