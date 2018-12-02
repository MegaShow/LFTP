package com.icytown.course.lftp.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Timer;

public class LGetClient extends ResendHandler {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filename;
    private Timer timer;
    private FileOutputStream fos;

    public LGetClient(String url, int port, String filename) throws UnknownHostException, SocketException {
        this.address = InetAddress.getByName(url);
        this.port = port;
        this.filename = filename;
        this.socket = new DatagramSocket(port, address);
        this.timer = new Timer();
    }

    public void run() throws IOException, LFTPException{
        // 文件传输前的准备
        sendLgetRequest();
        receiveResponse();
        sendFileRequest();

        File file = new File(filename);
        fos = new FileOutputStream(file);
        if(!file.exists()) {
            file.createNewFile();
        }

        // 开始文件传输
        FileReceiver fileReceiver = new FileReceiver(fos, socket, address, port);
        fileReceiver.run();
    }

    private void sendLgetRequest() throws IOException {
        // 发送文件请求
        byte[] data = filename.getBytes();
        Packet packet = new Packet(0);
        packet.setData(data);
        data = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(datagramPacket);

        // 添加定时器
        ResendTask resendTask = new ResendTask(timer, this, socket, packet, Utils.INITIAL_TIMEOUT_INTERVAL, address, port);
        packet.setResendTask(resendTask);
        timer.schedule(resendTask, Utils.INITIAL_TIMEOUT_INTERVAL);
    }

    private boolean receiveResponse() throws IOException, LFTPException{
        byte[] data = new byte[1400];
        DatagramPacket rawPacket = new DatagramPacket(data, data.length);
        socket.receive(rawPacket);
        Packet packet = Packet.fromBytes(rawPacket.getData());
        String response = new String(packet.getData());
        if(response == "yes") return true;
        throw new LFTPException("File doesn't exist on Server.");
    }

    private void sendFileRequest() throws IOException {
        // 发送开始传输的请求
        byte[] data = filename.getBytes();
        Packet packet = new Packet(0);
        packet.setData(data);
        data = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(datagramPacket);
    }
}
