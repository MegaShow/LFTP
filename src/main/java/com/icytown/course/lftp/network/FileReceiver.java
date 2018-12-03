package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

public class FileReceiver implements Runnable {

    private DatagramSocket socket;
    private String filename;

    private LinkedList<Packet> rcvBuffer = new LinkedList<>();
    private final int rcvBufLen = 10240;
    private int rcvWindow = rcvBufLen;
    private int lastSeqAcked;
    private int lastSeqRcvd;
    private int lastSeqRead;
    private boolean finish;

    public FileReceiver(DatagramSocket socket, String filename) {
        this.socket = socket;
        this.filename = filename;
    }

    @Override
    public void run() {
        new Thread(new FileWriteTask()).start();
        byte[] bytes = new byte[1400];
        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
        try {
            while (!finish) {
                socket.receive(datagramPacket);
                Packet packet = Packet.fromBytes(datagramPacket.getData());
                rcvWindow = rcvBufLen - lastSeqRcvd + lastSeqRead;
                if (packet != null) {
                    int id = packet.getId();
                    if (id == 0) {
                        byte[] ackData = new Packet(0, true, rcvWindow).getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                        socket.send(ackPacket);
                    } else if (id != lastSeqAcked + 1) {
                        // 接收到失序分组，重复发送上一次的ACK
                        byte[] ackData = new Packet(lastSeqAcked, true, rcvWindow).getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                        socket.send(ackPacket);
                    } else if (rcvWindow != 0 && id == lastSeqAcked + 1) {
                        // 如果接收窗口未满，则按序接收，进行累积确认
                        Console.out("Receive packet " + packet.getId());
                        synchronized (FileWriteTask.class) {
                            if (packet.isEnd()) {
                                finish = true;
                            } else {
                                rcvBuffer.addLast(packet);
                            }
                        }
                        lastSeqAcked++;
                        lastSeqRcvd++;
                        rcvWindow--;
                        byte[] ackData = new Packet(packet.getId(), true, rcvWindow).getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                        socket.send(ackPacket);
                    }
                }
            }
            SocketPool.removeSocket(datagramPacket.getAddress().getHostName() + ":" + datagramPacket.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class FileWriteTask implements Runnable {

        @Override
        public void run() {
            File file = new File(filename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (FileOutputStream fos = new FileOutputStream(file, false)) {
                while (!(finish && rcvBuffer.size() == 0)) {
                    synchronized (FileWriteTask.class) {
                        if (rcvBuffer.size() != 0) {
                            Packet packet = rcvBuffer.removeFirst();
                            fos.write(packet.getData());
                            lastSeqRead++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
