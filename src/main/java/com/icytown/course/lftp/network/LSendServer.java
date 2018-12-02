package com.icytown.course.lftp.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Timer;

public class LSendServer {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filename;
    private FileOutputStream fos;

    public LSendServer(InetAddress address, int port, String filename) throws SocketException, LFTPException {
        this.address = address;
        this.port = port;
        this.filename = filename;
        this.socket = new DatagramSocket(port, address);

        File file = new File(filename);
        try{
            fos = new FileOutputStream(file);
            if(!file.exists()) {
                file.createNewFile();
            }
            else {
                throw new LFTPException("File exists on Server.");
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            throw new LFTPException("Server failed to create file.");
        }
    }

    public void run() throws IOException {
        // 文件传输前的准备
        sendResponse();

        // 开始文件传输
        FileReceiver fileReceiver = new FileReceiver(fos, socket, address, port);
        fileReceiver.run();
    }

    private void sendResponse() throws IOException{
        String response = "yes";
        Packet packet = new Packet(0);
        packet.setData(response.getBytes());
        byte[] data = packet.getBytes();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }
}
