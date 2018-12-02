package com.icytown.course.lftp.network;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class LGetServer {
    DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filepath;
    private FileInputStream fis;

    public LGetServer(InetAddress address, int port, String filepath) throws SocketException, FileNotFoundException {
        this.address = address;
        this.port = port;
        this.filepath = filepath;

        socket = new DatagramSocket(port, address);
        File file = new File(filepath);
        fis = new FileInputStream(file);
    }

    public void run() throws IOException {
        // 文件传输前的准备
        sendResponse("yes");
        receiveResponse();

        // 开始文件传输
        FileSender fileSender = new FileSender(fis, socket, address, port);
        fileSender.run();
    }

    private void sendResponse(String response) throws IOException{
        Packet packet = new Packet(0);
        packet.setData(response.getBytes());
        byte[] data = packet.getBytes();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }

    private void receiveResponse() {

    }
}
