package com.icytown.course.lftp.network;

import java.io.*;

public class Packet implements Serializable {

    private int id;

    private boolean ack;
    private boolean end;

    private byte[] data;

    private transient OnCallbackListener onCallbackListener;

    public Packet(int id) {
        this.id = id;
    }

    public Packet(int id, boolean ack) {
        this.id = id;
        this.ack = ack;
    }

    public int getId() {
        return id;
    }

    public boolean isAck() {
        return ack;
    }

    public void setData(byte[] data) {
        this.data = data.clone();
    }

    public byte[] getData() {
        return data;
    }

    public void setOnCallbackListener(OnCallbackListener onCallbackListener) {
        this.onCallbackListener = onCallbackListener;
    }

    public static Packet fromBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return (Packet) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public byte[] getBytes() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            out.flush();
            byte[] bytes = bos.toByteArray();
            return bytes;
        } catch (IOException e) {
            return null;
        }
    }

    public interface OnCallbackListener {
        void onSuccess(Packet packet);
    }
}
