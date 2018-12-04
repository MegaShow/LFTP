package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.FileReceiver;
import com.icytown.course.lftp.network.PacketSocket;
import com.icytown.course.lftp.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.net.DatagramSocket;
import java.net.SocketException;


@Command(name = "lget", aliases = {"g"}, mixinStandardHelpOptions = true, description = "Download a file from server.")
public class LGet implements Runnable {

    @Parameters(index = "0", paramLabel = "server_url", description = "Server's url or ip.")
    private String url;

    @Parameters(index = "1", paramLabel = "file_name", description = "Filename which need to download.")
    private String filename;

    @Override
    public void run() {
        int port = 2333, index = url.indexOf(':');
        if (index != -1) {
            try {
                port = Integer.parseInt(url.substring(index + 1));
            } catch (NumberFormatException e) {
                Console.err("Server url '" + url + "' invalid.");
                return;
            }
            url = url.substring(0, index);
        }
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] bytes = PacketSocket.send(socket, url, port, 5000, ("Get," + filename).getBytes());
            if (bytes == null) {
                return;
            }
            String data = new String(bytes);
            if (data.equals("not_found")) {
                Console.err("No such file in server, cannot find '" + filename + "'.");
            } else if (data.contains("ok,")) {
                String[] parameters = data.split(",");
                DatagramSocket fileSocket = new DatagramSocket();
                byte[] fileBytes = PacketSocket.send(fileSocket, url, Integer.parseInt(parameters[1]), 5000, "Ready".getBytes());
                new Thread(new FileReceiver(fileSocket, filename, Long.parseLong(parameters[2]), false)).start();
            } else {
                Console.err("Unknown response data: " + data);
            }
        } catch (SocketException e) {
            Console.err("Create socket failed.");
        }
    }
}
