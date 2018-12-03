package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class FileSender implements Runnable {

    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filepath;

    private Timer speedTimer = new Timer();

    private final TimeOutTask timeOutTask = new TimeOutTask();
    private final AckTask ackTask = new AckTask();
    private int lastSeqAcked;                   // 上一个被确认的序号
    private int lastSeqSent = 0;                // 上一个已发送的序号
    private int rcvWindow = 1024;               // 接收窗口
    private int cWindow = 1;                    // 拥塞窗口
    private int ssthresh = 10;
    private Map<Integer, Packet> notAckedBuffer = new HashMap<>(); // 缓存已发送、未确认的分组
    private boolean finish = false;

    public FileSender(DatagramSocket socket, InetAddress address, int port, String filepath) {
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.filepath = filepath;
    }

    @Override
    public void run() {
        try (FileInputStream fis = new FileInputStream(new File(filepath))) {
            int read = 0;
            new Thread(ackTask).start();
            new Thread(timeOutTask).start();
            speedTimer.schedule(new SpeedTask(), 1000, 1000);
            while (read != -1) {
                synchronized (ackTask) {
                    if (rcvWindow != 0) {
                        boolean flag;
                        synchronized (FileSender.class) {
                            if (lastSeqSent - lastSeqAcked <= cWindow) {
                                lastSeqSent++;
                                Packet packet = new Packet(lastSeqSent, false);
                                byte[] data = new byte[1024];
                                read = fis.read(data);
                                if (read == -1) {
                                    packet.setEnd(true);
                                    finish = true;
                                } else {
                                    packet.setData(Arrays.copyOf(data, read));
                                }
                                notAckedBuffer.put(packet.getId(), packet);
                                byte[] bytes = packet.getBytes();
                                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address, port);
                                socket.send(datagramPacket);
                                // Console.out("Send to " + address.getHostName() + ":" + port + ", with packet " + id + ".");
                            }
                        }
                    } else {
                        // 接收窗口满时，发送只有一个字节数据的报文段，其seq为0
                        Packet packet = new Packet(0);
                        byte[] data = packet.getBytes();
                        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
                        socket.send(datagramPacket);
                    }
                }
            }
            SocketPool.removeSocket(address.getHostName() + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class AckTask implements Runnable {

        private int duplicateAck = 0;

        @Override
        public void run() {
            byte[] bytes = new byte[1400];
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
            while (!(finish && notAckedBuffer.size() == 0)) {
                try {
                    socket.receive(datagramPacket);
                    Packet packet = Packet.fromBytes(datagramPacket.getData());
                    if (packet != null) {
                        synchronized (AckTask.this) {
                            rcvWindow = packet.getRcvWindow();
                        }
                        if (packet.getId() == lastSeqAcked + 1) {
                            notAckedBuffer.remove(packet.getId());
                            synchronized (AckTask.this) {
                                lastSeqAcked++;
                            }
                            synchronized (FileSender.this) {
                                if (cWindow > ssthresh) {
                                    cWindow++;
                                } else {
                                    cWindow <<= 1;
                                }
                            }
                            duplicateAck = 0;
                            timeOutTask.updateTime();
                            // Console.out("Receive from " + address.getHostName() + ":" + port + ", with ack " + packet.getId() + ".");
                        } else if (packet.getId() == lastSeqAcked) {
                            duplicateAck++;
                        }
                        if (duplicateAck == 3) {
                            synchronized (FileSender.this) {
                                ssthresh = cWindow / 2;
                                cWindow = ssthresh + 3;
                            }
                            timeOutTask.quickResend();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class TimeOutTask implements Runnable {

        private long time;

        public void quickResend() {
            synchronized (TimeOutTask.this) {
                time = 0;
            }
        }

        public void updateTime() {
            synchronized (TimeOutTask.this) {
                time = System.currentTimeMillis();
            }
        }

        public long getTime() {
            synchronized (TimeOutTask.this) {
                return time;
            }
        }

        @Override
        public void run() {
            updateTime();
            while (!(finish && notAckedBuffer.size() == 0)) {
                if (System.currentTimeMillis() - getTime() > 500 && lastSeqAcked + 1 <= lastSeqSent) {
                    // Console.out("Time out, try to resend the packets from " + (lastSeqAcked + 1) + " to " + lastSeqSent + ".");
                    synchronized (FileSender.this) {
                        ssthresh = cWindow / 2;
                        cWindow = 1;
                        for (int i = lastSeqAcked + 1; i <= lastSeqSent; i++) {
                            try {
                                byte[] bytes = notAckedBuffer.get(i).getBytes();
                                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address, port);
                                socket.send(datagramPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        updateTime();
                    }
                }
            }
        }
    }

    public class SpeedTask extends TimerTask {

        private long time = 0;
        private long packet;

        @Override
        public void run() {
            time++;
            long speed;
            synchronized (ackTask) {
                speed = lastSeqAcked - packet;
                packet = lastSeqAcked;
            }
            Console.progress(packet, speed, ((float) packet) / time);
            if (finish && notAckedBuffer.size() == 0) {
                speedTimer.cancel();
            }
        }
    }
}
