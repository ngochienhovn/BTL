package com.ltnc.auction.server;

<<<<<<< HEAD
import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.db.SchemaInitializer;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.services.AuctionService;
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.WalletService;

public class ServerMain 
{
    public static void main(String[] args) 
    {
        int port = 5555;

        if (args.length >= 1) 
        {
            port = Integer.parseInt(args[0]);
        }

        System.out.println("Starting server on port " + port);
        SchemaInitializer.initialize();

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();
        WalletDAO walletDAO = new WalletDAO();
        WalletTransactionDAO walletTransactionDAO = new WalletTransactionDAO();

        AuthService authService = new AuthService(userDAO);
        ItemService itemService = new ItemService(itemDAO, authService);
        AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, userDAO, walletDAO, walletTransactionDAO);
        WalletService walletService = new WalletService(walletDAO, walletTransactionDAO);

        SocketServer server = new SocketServer(port, authService, itemService, auctionService, walletService);
        server.start();
    }
}
=======
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

>>>>>>> a7b09fa9049b4b79b22aea4f1f7fefb77ed2db5c
