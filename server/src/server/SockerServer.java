package com.ltnc.auction.server.server;

import java.io.*;
import java.net.*;

public class SockerServer 
{
    private final int port;

    public SockerServer(int port)
    {
        this.port = port;
    }

    public void start() throws IOException
    {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Server started on port " + port);

            while(true)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.setDaemon(true);
                t.start();
            }
        }
    }
}
