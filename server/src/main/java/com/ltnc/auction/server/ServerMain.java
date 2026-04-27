package com.ltnc.auction.server;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
<<<<<<< Updated upstream
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.service.AuctionService;
=======
import com.ltnc.auction.server.db.SchemaInitializer;
import com.ltnc.auction.server.network.AuctionBroadcaster;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.services.AuctionService;
import com.ltnc.auction.server.services.AuctionStateManager;
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.WalletService;
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
        SocketServer server = new SocketServer(port, auctionService);
        server.start(); // Blocking
=======
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();
        AuctionStateManager stateManager = new AuctionStateManager(auctionDAO, broadcaster);

        AuctionService auctionService = new AuctionService(
            auctionDAO, bidDAO, walletDAO, walletTransactionDAO, broadcaster  // NEW param
        );

        SocketServer server = new SocketServer(port, authService, itemService, auctionService, walletService);
        server.start();
>>>>>>> Stashed changes
    }
}