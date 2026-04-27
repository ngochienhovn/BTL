package com.ltnc.auction.server.network; // cả file này là sprint 4

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.ltnc.auction.shared.protocol.ServerToClientMessage; // phải có import này vì ServerToClientMessage ở file khác

public class AuctionBroadcaster {

    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public void register(ClientHandler client) {
        clients.add(client);
    }

    public void unregister(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(ServerToClientMessage message) {
        for (ClientHandler client : clients) {
            client.sendBroadcast(message);
        }
    }
}