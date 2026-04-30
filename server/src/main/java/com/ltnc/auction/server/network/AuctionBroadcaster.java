package com.ltnc.auction.server.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionBroadcaster {

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    public void broadcast(String message) {
        messages.add(message);
    }

    public void broadcast(Object message) {
        if (message != null) {
            broadcast(message.toString());
        }
    }

    public List<String> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }
}