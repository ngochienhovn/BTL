package com.ltnc.auction.server;

import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.db.SchemaInitializer;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;

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
        AuthService authService = new AuthService(userDAO);
        ItemService itemService = new ItemService(itemDAO, authService);

        SocketServer server = new SocketServer(port, authService, itemService);
        server.start();
    }
}