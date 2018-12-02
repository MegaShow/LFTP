package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.Server;
import com.icytown.course.lftp.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "serve", mixinStandardHelpOptions = true, description = "Listen and serve at a port.")
public class Serve implements Runnable {

    @Option(names = "-p", paramLabel = "port", defaultValue = "2333", description = "Port for server.")
    private int port;

    @Parameters(index = "0", paramLabel = "folder", defaultValue = "data/", description = "Data folder for server.")
    private String folderName;

    @Override
    public void run() {
        // 判断'folderName'是否存在
        File folder = new File(folderName);
        if (!folder.exists()) {
            Console.err("Serve failed, '" + folder.getAbsolutePath() + "' doesn't exist.");
            return;
        } else if (!folder.isDirectory()) {
            Console.err("Serve failed, '" + folder.getAbsolutePath() + "' is not directory.");
            return;
        }
        System.out.println("Serve at '" + folder.getAbsolutePath() + "'.");

        // 监听端口，等待客户端的连接
        Server server = new Server(port);
        if (!server.canServe()) {
            Console.err("Listen at port '" + port + "' failed.");
            return;
        }
        Console.out("Listen at port '" + port + "'.");
        boolean status = server.serve(folder.getAbsolutePath());
        if (!status) {
            Console.err("Receive packet failed.");
        }
    }
}
