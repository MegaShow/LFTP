package com.icytown.course.lftp.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

    private DatagramSocket socket;
    private int sequence;

    public Server(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            socket = null;
        }
    }

    public boolean canServe() {
        return socket != null;
    }

    public boolean serve(String folderPath) {
        while (true) {
            byte[] data = new byte[1400];
            DatagramPacket rawPacket = new DatagramPacket(data, data.length);
            try {
                socket.receive(rawPacket);
                Packet packet = Packet.fromBytes(rawPacket.getData());
                if (packet != null) {
                    String parameters = new String(packet.getData());
                    System.out.println(parameters);
                    String type = parameters.substring(0, parameters.indexOf(','));
                    String filename = parameters.substring(parameters.indexOf(',') + 1);
                    if(type == "lget") {
                        LGetServer lGetServer = new LGetServer(rawPacket.getAddress(), rawPacket.getPort(), folderPath + "/" + filename);
                        lGetServer.run();
                    }
                    else {
                        LSendServer lSendServer = new LSendServer(rawPacket.getAddress(), rawPacket.getPort(), folderPath + "/" + filename);
                        lSendServer.run();
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("File doesn't exist on server.");
            } catch (IOException e) {
                System.err.println("Server failed to transfer or write.");
            } catch (LFTPException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
