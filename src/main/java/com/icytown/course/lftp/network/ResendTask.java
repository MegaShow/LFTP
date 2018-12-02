package com.icytown.course.lftp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class ResendTask extends TimerTask {
    Timer timer;
    ResendHandler handler;
    DatagramSocket socket;
    Packet packet;
    long timeoutInterval;
    InetAddress address;
    int port;

    public ResendTask(Timer timer, ResendHandler handler, DatagramSocket socket, Packet packet, long timeoutInterval, InetAddress address, int port) {
        this.timer = timer;
        this.handler = handler;
        this.socket = socket;
        this.packet = packet;
        this.timeoutInterval = timeoutInterval;
        this.address = address;
        this.port = port;
    }

    public long getTimeoutInterval() {
        return timeoutInterval;
    }

    @Override
    public void run() {
        try{
            // 重传
            byte[] data = packet.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
            socket.send(datagramPacket);

            // 重置冗余ACK数
            handler.resetAck();
            // 超时间隔倍增，重置计时器
            ResendTask resendTask = new ResendTask(timer, handler, socket, packet, timeoutInterval * 2, address, port);
            packet.setResendTask(resendTask);
            timer.schedule(resendTask, timeoutInterval * 2);

        } catch (IOException e) {
            handler.collectionResendException(e);
        }
    }
}
