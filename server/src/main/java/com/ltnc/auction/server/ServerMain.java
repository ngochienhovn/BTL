package com.ltnc.auction.server;

import com.ltnc.auction.server.network.SocketServer;

public class ServerMain {
  public static void main(String[] args) {
    int port = 5555;
    if (args.length >= 1) {
      port = Integer.parseInt(args[0]);
    }

    SocketServer server = new SocketServer(port);
    server.start(); // Blocking
  }
}

