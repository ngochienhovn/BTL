package com.ltnc.auction.server;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.service.AuctionService;

public class ServerMain {
    public static void main(String[] args) {
        int port = 5555;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();
        WalletDAO walletDAO = new WalletDAO();
        WalletTransactionDAO walletTransactionDAO = new WalletTransactionDAO();

        AuctionService auctionService = new AuctionService(
                auctionDAO,
                bidDAO,
                walletDAO,
                walletTransactionDAO
        );

        SocketServer server = new SocketServer(port, auctionService);
        server.start(); // Blocking
    }
}