package com.ltnc.auction.server.network;

import java.io.PrintWriter;

/** One TCP client connection; userId set after successful login. */
public class ClientSession {
    public final PrintWriter writer;
    public volatile Long userId;
    public volatile String email;

    public ClientSession(PrintWriter writer) {
        this.writer = writer;
    }
}
