package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class FileReceiver implements Runnable {

    private DatagramSocket socket;
    private String url;
    private String filename;
    private long filelength;
    private boolean server;

    private Timer speedTimer = new Timer();
    private Timer finishTimer = new Timer();
    private final SpeedTask speedTask = new SpeedTask();
    private FinishTimeOutTask finishTimeOutTask;

    private LinkedList<Packet> rcvBuffer = new LinkedList<>();
    private final int rcvBufLen = 10240;
    private int rcvWindow = rcvBufLen;
    private int lastSeqAcked;
    private int lastSeqRead;
    private boolean finish;

    public FileReceiver(DatagramSocket socket, String url, String filename, long filelength, boolean server) {
        this.socket = socket;
        this.url = url;
        this.filename = filename;
        this.filelength = filelength;
        this.server = server;
    }

    @Override
    public void run() {
        new Thread(new FileWriteTask()).start();
        byte[] bytes = new byte[1400];
        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
        if (!server) {
            speedTimer.schedule(speedTask, 1000, 1000);
        }
        try {
            while (!finish) {
                socket.receive(datagramPacket);
                Packet packet = Packet.fromBytes(datagramPacket.getData());
                synchronized (FileReceiver.this) {
                    rcvWindow = rcvBufLen - rcvBuffer.size();
                    if (packet != null) {
                        int id = packet.getId();
                        if (id == 0) {
                            byte[] ackData = new Packet(0, true, rcvWindow).getBytes();
                            DatagramPacket ackDatagramPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                            socket.send(ackDatagramPacket);
                        } else if (id != lastSeqAcked + 1) {
                            // 接收到失序分组，重复发送上一次的ACK
                            byte[] ackData = new Packet(lastSeqAcked, true, rcvWindow).getBytes();
                            DatagramPacket ackDatagramPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                            socket.send(ackDatagramPacket);
                        } else if (rcvWindow != 0 && id == lastSeqAcked + 1) {
                            // 如果接收窗口未满，则按序接收，进行累积确认
                            // Console.out("Receive packet " + packet.getId());
                            Packet ackPacket = new Packet(packet.getId(), true, rcvWindow);
                            if (packet.isEnd()) {
                                ackPacket.setEnd(true);
                                if (finishTimeOutTask == null) {
                                    finishTimeOutTask = new FinishTimeOutTask();
                                    finishTimer.schedule(finishTimeOutTask, server ? 3000 : 1000);
                                }
                            } else if (packet.isFin()) {
                                ackPacket.setFin(true);
                                finish = true;
                                finishTimer.cancel();
                                finishTimeOutTask.finish();
                                break;
                            } else {
                                rcvBuffer.addLast(packet);
                            }
                            lastSeqAcked++;
                            rcvWindow--;
                            byte[] ackData = ackPacket.getBytes();
                            DatagramPacket ackDatagramPacket = new DatagramPacket(ackData, ackData.length, datagramPacket.getAddress(), datagramPacket.getPort());
                            socket.send(ackDatagramPacket);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // 接收到异常，破坏阻塞，退出线程
            // e.printStackTrace();
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
                    synchronized (FileReceiver.this) {
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

    public class FinishTimeOutTask extends TimerTask {

        public void finish() {
            synchronized (FileReceiver.this) {
                finish = true;
                if (server) {
                    SocketPool.removeSocket(url);
                } else {
                    long time = speedTask.show();
                    speedTimer.cancel();
                    Console.progressFinish(time, filelength);
                }
                Console.out("Receive file from " + url + " successfully.");
            }
        }

        @Override
        public void run() {
            finish();
        }
    }

    public class SpeedTask extends TimerTask {

        private long time = 0;
        private long packet;

        private long show() {
            time++;
            long speed, progress;
            synchronized (FileReceiver.this) {
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
            if (finish) {
                speedTimer.cancel();
            }
        }
    }
}
