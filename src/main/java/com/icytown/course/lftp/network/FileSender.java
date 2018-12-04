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
    private long filelength;
    private boolean server;

    private Timer speedTimer = new Timer();
    private Timer finishTimer = new Timer();
    private final SpeedTask speedTask = new SpeedTask();
    private final TimeOutTask timeOutTask = new TimeOutTask();
    private final AckTask ackTask = new AckTask();

    private int lastSeqAcked = 0;               // 上一个被确认的序号
    private int lastSeqSent = 0;                // 上一个已发送的序号
    private int rcvWindow = 10240;              // 接收窗口
    private double cWindow = 1;                 // 拥塞窗口
    private int ssthresh = 10;
    private Map<Integer, Packet> notAckedBuffer = new HashMap<>(); // 缓存已发送、未确认的分组
    private boolean finish = false;

    public FileSender(DatagramSocket socket, InetAddress address, int port, String filepath, long filelength, boolean server) {
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.filepath = filepath;
        this.filelength = filelength;
        this.server = server;
    }

    @Override
    public void run() {
        try (FileInputStream fis = new FileInputStream(new File(filepath))) {
            int read = 0;
            new Thread(ackTask).start();
            new Thread(timeOutTask).start();
            if (!server) {
                speedTimer.schedule(speedTask, 1000, 1000);
            }
            while (read != -1) {
                synchronized (FileSender.class) {
                    if (rcvWindow != 0) {
                        if (lastSeqSent - lastSeqAcked <= rcvWindow && lastSeqSent - lastSeqAcked <= cWindow) {
                            lastSeqSent++;
                            Packet packet = new Packet(lastSeqSent, false);
                            byte[] data = new byte[1024];
                            read = fis.read(data);
                            if (read == -1) {
                                packet.setEnd(true);
                            } else {
                                packet.setData(Arrays.copyOf(data, read));
                            }
                            notAckedBuffer.put(packet.getId(), packet);
                            byte[] bytes = packet.getBytes();
                            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address, port);
                            socket.send(datagramPacket);
                            // Console.out("Send to " + address.getHostName() + ":" + port + ", with packet " + packet.getId() + ".");
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
            finishTimer.schedule(new FinishTimeOutTask(), 10000);
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
            while (!finish) {
                try {
                    socket.receive(datagramPacket);
                    Packet packet = Packet.fromBytes(datagramPacket.getData());
                    if (packet != null) {
                        synchronized (AckTask.this) {
                            rcvWindow = packet.getRcvWindow();
                        }
                        if (packet.getId() >= lastSeqAcked + 1) {
                            // Console.out("Receive from " + address.getHostName() + ":" + port + ", with ack " + packet.getId() + ".");
                            synchronized (FileSender.this) {
                                for (int i = lastSeqAcked + 1; i <= packet.getId(); i++) {
                                    notAckedBuffer.remove(i);
                                }
                                lastSeqAcked = packet.getId();
                                if (packet.isEnd()) {
                                    Packet fin = new Packet(lastSeqSent + 1);
                                    fin.setFin(true);
                                    byte[] finData = fin.getBytes();
                                    DatagramPacket finDatagramPacket = new DatagramPacket(finData, finData.length, address, port);
                                    socket.send(finDatagramPacket);
                                    finish = true;
                                    finishTimer.cancel();
                                    new Thread(new FinishTimeOutTask()).start();
                                    break;
                                }
                                if (cWindow > ssthresh) {
                                    cWindow = cWindow + 1.0f / cWindow;
                                } else {
                                    cWindow *= 2;
                                }
                            }
                            duplicateAck = 0;
                            timeOutTask.updateTime();
                        } else if (packet.getId() == lastSeqAcked) {
                            duplicateAck++;
                        }
                        if (duplicateAck == 3) {
                            synchronized (FileSender.this) {
                                ssthresh = (int) Math.round(cWindow / 2);
                                cWindow = ssthresh + 3;
                                timeOutTask.resend();
                            }
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

        public void resend() {
            synchronized (FileSender.this) {
                if (rcvWindow != 0 && lastSeqAcked + 1 <= lastSeqSent) {
                    int finalSeq = Math.min(lastSeqAcked + rcvWindow - 1, lastSeqSent);
                    // Console.out("Time out, try to resend the packets from " + (lastSeqAcked + 1) + " to " + finalSeq + ".");
                    // Console.out("" + lastSeqAcked + " " + rcvWindow + " " + lastSeqSent + " " + cWindow + " " + ssthresh);
                    if (lastSeqSent - lastSeqAcked <= rcvWindow) {
                        for (int i = lastSeqAcked + 1; i <= finalSeq; i++) {
                            try {
                                if (notAckedBuffer.containsKey(i)) {
                                    byte[] bytes = notAckedBuffer.get(i).getBytes();
                                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address, port);
                                    socket.send(datagramPacket);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
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
            while (!finish) {
                if (System.currentTimeMillis() - getTime() > 300) {
                    synchronized (FileSender.this) {
                        ssthresh = (int) Math.round(cWindow / 2);
                        cWindow = 1;
                        int finalSeq = Math.min(lastSeqAcked + rcvWindow, lastSeqSent);
                        // Console.out("Time out, try to resend the packets from " + (lastSeqAcked + 1) + " to " + finalSeq + ".");
                    }
                    resend();
                    updateTime();
                }
            }
        }
    }

    public class FinishTimeOutTask extends TimerTask {

        @Override
        public void run() {
            synchronized (FileSender.this) {
                finish = true;
                SocketPool.removeSocket(address.getHostName() + ":" + port);
                if (!server) {
                    long time = speedTask.show();
                    speedTimer.cancel();
                    Console.progressFinish(time, filelength);
                }
                Console.out("Send file to " + address.getHostName() + ":" + port + " successfully.");
            }
        }
    }

    public class SpeedTask extends TimerTask {

        private long time = 0;
        private long packet;

        private long show() {
            time++;
            long speed, progress;
            synchronized (FileSender.this) {
                speed = lastSeqAcked - packet;
                packet = lastSeqAcked;
                progress = packet * 100 / filelength;
            }
            Console.progress(progress, speed, filelength);
            return time;
        }

        @Override
        public void run() {
            show();
            if (finish && notAckedBuffer.size() == 0) {
                speedTimer.cancel();
            }
        }
    }
}
