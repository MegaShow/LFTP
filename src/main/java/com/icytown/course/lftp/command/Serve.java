package com.icytown.course.lftp.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "serve", mixinStandardHelpOptions = true, description = "Listen and serve at a port.")
public class Serve implements Runnable {

    @Option(names = "-p", paramLabel = "port", defaultValue = "2333", description = "Port for server.")
    private int port;

    @Override
    public void run() {
        System.out.println("Call Serve");
        System.out.println("Port: " + port);
    }
}
