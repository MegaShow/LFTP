package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.*;
import com.icytown.course.lftp.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

@Command(name = "lsend", aliases = {"s"}, mixinStandardHelpOptions = true, description = "Upload a file to server.")
public class LSend implements Runnable {

    @Parameters(index = "0", paramLabel = "server_url", description = "Server's url or ip.")
    private String url;

    @Parameters(index = "1", paramLabel = "file_name", description = "Filename which need to upload.")
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
        File file = new File(filename);
        if (!file.exists()) {
            Console.err("File '" + file.getAbsolutePath() + "' doesn't exist.");
            return;
        }
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] bytes = PacketSocket.send(url, port, 5000, ("Send," + socket.getLocalPort() + "," + filename).getBytes());
            if (bytes == null) {
                return;
            }
            String data = new String(bytes);
            if (data.equals("exist")) {
                Console.err("Same filename exist in server, as '" + filename + "'.");
            } else if (data.contains("ok,")) {
                String[] parameters = data.split(",");
                new Thread(new FileSender(socket, InetAddress.getByName(url), Integer.parseInt(parameters[1]), filename)).start();
            } else {
                Console.err("Unknown response data: " + data);
            }
        } catch (SocketException | UnknownHostException e) {
            Console.err("Create socket failed.");
        }
    }
}
