package com.icytown.course.lftp.network;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class FileReceiver {
    private FileOutputStream fos;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    private int lastSeqAcked;
    private int rcvBufLen = Utils.WINDOW_MAX_LENGTH;
    private int lastSeqRcvd;
    private int lastSeqRead;
    private ArrayList<Packet> rcvBuffer;

    public FileReceiver(FileOutputStream fos, DatagramSocket socket, InetAddress address, int port){
        this.fos = fos;
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.rcvBuffer = new ArrayList<>(rcvBufLen);
    }

    public void run() throws IOException{
        while(true) {
            byte[] data = new byte[1400];
            DatagramPacket rawPacket = new DatagramPacket(data, data.length);
            socket.receive(rawPacket);
            int rcvWindow = rcvBufLen - lastSeqRcvd + lastSeqRead;
            Packet packet = Packet.fromBytes(rawPacket.getData());
            if(packet != null) {
                System.out.println(new String(packet.getData()));
                int id = packet.getId();
                if(id == 0) {
                    byte[] ackData = new Packet(0, true, rcvWindow).getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                    socket.send(ackPacket);
                } else if(id != lastSeqAcked + 1) {
                    // 接收到失序分组，重复发送上一次的ACK
                    byte[] ackData = new Packet(lastSeqAcked, true, rcvWindow).getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                    socket.send(ackPacket);
                } else if(rcvWindow != 0 && id == lastSeqAcked + 1) {
                    // 如果接收窗口未满，则按序接收，进行累积确认
                    rcvBuffer.add(packet);
                    lastSeqAcked++;
                    lastSeqRcvd++;
                    rcvWindow--;
                    byte[] ackData = new Packet(packet.getId(), true, rcvWindow).getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                    socket.send(ackPacket);

                    // 从rcvBuffer中读取数据
                    Packet towrite = rcvBuffer.remove(0);
                    fos.write(towrite.getData());
                    lastSeqRead++;
                }
            }
        }
    }
}
