package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

public class Server {

    private DatagramSocket socket;

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
        byte[] data = new byte[1400];
        DatagramPacket rawPacket = new DatagramPacket(data, data.length);
        while (true) {
            try {
                socket.receive(rawPacket);
                Packet packet = Packet.fromBytes(rawPacket.getData());
                if (packet != null) {
                    String url = rawPacket.getAddress().getHostName() + ":" + rawPacket.getPort();
                    Console.out("Recieve request from " + url);
                    String[] parameters = new String(packet.getData()).split(",");
                    if (parameters[0].equals("Get")) {
                        File file = new File(folderPath, parameters[1]);
                        Packet ack = new Packet(packet.getId(), true);
                        if (!file.exists()) {
                            ack.setData("not_found".getBytes());
                            Console.err(url + " want to download '" + file.getAbsolutePath() + "', but not found.");
                        } else {
                            DatagramSocket socket = SocketPool.getSocket(url);
                            if (socket == null) {
                                ack.setData("failed".getBytes());
                                Console.err(url + " want to download '" + file.getAbsolutePath() + "', but alloc socket failed.");
                            } else {
                                ack.setData(("ok," + socket.getPort()).getBytes());
                                Console.out(url + " want to download '" + file.getAbsolutePath() + "', allowed.");
                                new Thread(new SendTask(socket, rawPacket.getAddress(), rawPacket.getPort(), file.getPath())).start();
                            }
                        }
                        byte[] ackData = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                        socket.send(ackPacket);
                    }
//                        LGetServer lGetServer = new LGetServer(rawPacket.getAddress(), rawPacket.getPort(), folderPath + "/" + filename);
//                        lGetServer.run();
//                    }
//                    else {
//                        LSendServer lSendServer = new LSendServer(rawPacket.getAddress(), rawPacket.getPort(), folderPath + "/" + filename);
//                        lSendServer.run();
//                    }
                }
            } catch (FileNotFoundException e) {
                Console.err("File doesn't exist on server.");
            } catch (IOException e) {
                Console.err("Server failed to transfer or write.");
//            } catch (LFTPException e) {
//                Console.err.println(e.getMessage());
            }
        }
    }
}
