package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.LFTPException;
import com.icytown.course.lftp.network.LGetClient;
import com.icytown.course.lftp.network.LSendClient;
import com.icytown.course.lftp.network.LSendServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
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
        System.out.println("Call LSend");
        System.out.println("Url: " + url);
        System.out.println("Filename: " + filename);

        int port = 2333, index = url.indexOf(':');
        if (index != -1) {
            try {
                port = Integer.parseInt(url.substring(index + 1));
            } catch (NumberFormatException e) {
                System.err.println("Server url '" + url + "' invalid.");
                return;
            }
            url = url.substring(0, index);
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            System.err.println("Send failed, unknown host.");
            return;
        }

        try{
            LSendClient lSendClient = new LSendClient(address, port, filename);
            lSendClient.run();
        } catch (SocketException e) {
            System.err.println("Send failed, can not create socket.");
        } catch (IOException e) {
            System.err.println("Send failed.");
        } catch (LFTPException e) {
            System.err.println(e.getMessage());
        }
    }
}
