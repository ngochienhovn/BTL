package com.ltnc.auction.server.network;

import java.io.PrintWriter;
import java.net.Socket;

/** One TCP client connection; userId set after successful login. */
public class ClientSession {
    public final Socket socket;
    public final PrintWriter writer;
    public volatile Long userId;
    public volatile String email;
    public volatile long lastActiveTime;

    public ClientSession(Socket socket, PrintWriter writer) {
        this.socket = socket;
        this.writer = writer;
        this.lastActiveTime = System.currentTimeMillis();
    }
}
