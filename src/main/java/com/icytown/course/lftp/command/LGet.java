package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.PacketSocket;
import com.icytown.course.lftp.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


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
        byte[] bytes = PacketSocket.send(url, port, 5000, ("Get," + filename).getBytes());
        if (bytes == null) {
            return;
        }
        String data = new String(bytes);
        if (data.equals("not_found")) {
            Console.err("No such file in server, cannot find '" + filename + "'.");
        } else if (data.contains("ok,")) {
            Console.out("ok");
        } else {
            Console.err("Unknown response data: " + data);
        }

        /*
        PacketSocket.sendNowAsync(url, port, filename, packet -> {
            Console.out.println("Send packet successfully.");
        });
        */

//        try{
//            LGetClient lGetClient = new LGetClient(url, port, filename);
//            lGetClient.run();
//        } catch (UnknownHostException e) {
//            Console.err.println("Send failed, unknown host.");
//        } catch (SocketException e) {
//            Console.err.println("Send failed, can not create socket.");
//        } catch (IOException e) {
//            Console.err.println("Transfer or write failed.");
//        } catch (LFTPException e) {
//            Console.err.println(e.getMessage());
//        }
    }
}
