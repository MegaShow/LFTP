package com.icytown.course.lftp.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class LSendClient extends ResendHandler{
    DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filepath;
    private FileSender fileSender;
    private FileInputStream fis;

    public LSendClient(InetAddress address, int port, String filepath) throws SocketException, FileNotFoundException {
        this.address = address;
        this.port = port;
        this.filepath = filepath;

        socket = new DatagramSocket(port, address);
        File file = new File(filepath);
        fis = new FileInputStream(file);
    }

    public void run() throws IOException, LFTPException {
        // 文件传输前的准备
        receiveResponse();

        // 开始文件传输
        FileSender fileSender = new FileSender(fis, socket, address, port);
        fileSender.run();
    }

    private void receiveResponse() throws IOException, LFTPException{
        byte[] data = new byte[1400];
        DatagramPacket rawPacket = new DatagramPacket(data, data.length);
        socket.receive(rawPacket);
        Packet packet = Packet.fromBytes(rawPacket.getData());
        String response = new String(packet.getData());
        if(response != "yes") throw new LFTPException("File exists on Server.");
    }
}
