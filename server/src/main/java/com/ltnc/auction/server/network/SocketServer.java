package com.ltnc.auction.server.network;

import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.AuctionService;
import com.ltnc.auction.server.services.WalletService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private final int port;
    private final AuthService authService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final WalletService walletService;
    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    public SocketServer(
            int port,
            AuthService authService,
            ItemService itemService,
            AuctionService auctionService,
            WalletService walletService
    ) {
        this.port = port;
        this.authService = authService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.walletService = walletService;
    }

    public void start() {
        System.out.println("[server] Starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new ClientHandler(socket, authService, itemService, auctionService, walletService));
            }
        } catch (IOException e) {
            throw new RuntimeException("Server failed to start", e);
        }
    }
}
