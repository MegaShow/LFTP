package com.icytown.course.lftp.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class FileSender extends ResendHandler {
    private FileInputStream fis;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private int lastSeqAcked;                   // 上一个被确认的序号
    private int lastSeqSent = 0;                // 上一个已发送的序号
    private int rcvWindow;                      // 接收窗口
    private ArrayList<Packet> notAckedBuffer;   // 缓存已发送、未确认的分组
    private Timer timer;
    private int endSeq = -1;

    public FileSender(FileInputStream fis, DatagramSocket socket, InetAddress address, int port){
        this.fis = fis;
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.notAckedBuffer = new ArrayList<>();
        this.timer = new Timer();
    }

    public void run() throws IOException{

        // 发送N个分组，直到发送窗口已满，或者读完文件
        for(int i = 0; i < Utils.WINDOW_MAX_LENGTH; i++) {
            if(sendNew()) break;
        }

        while(!receive()) {
            if(getResendException() != null) throw getResendException();
            if(getDuplicateAck() == 3) resend();    // 快速重传
            else send();
            if(getResendException() != null) throw getResendException();
        }
    }

    private void send() throws IOException{
        // 如果窗口已满，或文件数据已全部发送，不进行发送操作
        if(lastSeqSent - lastSeqAcked >= Utils.WINDOW_MAX_LENGTH) return;
        if(rcvWindow == 0) {
            controlFlow();
        } else {
            sendNew();
        }
    }

    private void controlFlow() throws IOException{
        // 接收窗口满时，发送只有一个字节数据的报文段，其seq为0
        byte[] data = {0};
        Packet packet = new Packet(0);
        packet.setData(data);
        data = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(datagramPacket);
    }

    private boolean sendNew() throws IOException{
        // 发送

        // 读取文件数据
        FileInputStream fis = null;
        byte[] data = new byte[1400];
        int sign = fis.read(data);
        lastSeqSent++;

        Packet packet = new Packet(lastSeqSent);
        // 要判断是否已读到文件尾
        if(sign == -1) {
            packet.setEnd(true);
            endSeq = lastSeqSent;
        }

        packet.setData(data);
        data = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(datagramPacket);

        // 添加定时器
        ResendTask resendTask = new ResendTask(timer, this, socket, packet, Utils.INITIAL_TIMEOUT_INTERVAL, address, port);
        packet.setResendTask(resendTask);
        timer.schedule(resendTask, Utils.INITIAL_TIMEOUT_INTERVAL);
        // 更新已发送、未确认的队列
        notAckedBuffer.add(packet);

        return sign == -1;
    }

    private void resend() throws IOException{
        Packet packet = notAckedBuffer.get(0);
        // 重传
        byte[] data = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(datagramPacket);

        // 重置冗余ACK数
        resetAck();
        // 超时间隔倍增，重置计时器
        ResendTask resendTask = new ResendTask(timer, this, socket, packet,
                packet.getResendTask().getTimeoutInterval() * 2, address, port);
        packet.setResendTask(resendTask);
        timer.schedule(resendTask, resendTask.getTimeoutInterval());
    }

    private boolean receive() throws IOException{
        byte[] data = new byte[1400];
        DatagramPacket rawPacket = new DatagramPacket(data, data.length);
        socket.receive(rawPacket);
        Packet packet = Packet.fromBytes(rawPacket.getData());
        if (packet != null) {
            System.out.println(new String(packet.getData()));

            // 更新接收窗口
            rcvWindow = packet.getRcvWindow();

            if(packet.getId() == lastSeqAcked) {        // 冗余ACK

                addAck();

            } else if(packet.getId() == endSeq) {       // 传输完毕
                // TODO
                return true;

            } else if(packet.getId() != 0) {            // 新的ACK

                // 更新上一个已确认的序号
                lastSeqAcked = packet.getId();

                // 重置冗余ACK计数
                resetAck();

                // 取消定时器，更新未确认队列
                int index = 0;
                while(index != notAckedBuffer.size()) {
                    Packet p = notAckedBuffer.get(index);
                    if(p.getId() > lastSeqAcked) break;
                    p.cancelResendTask();
                    notAckedBuffer.remove(index);
                }
            }
        }
        return false;
    }
}
