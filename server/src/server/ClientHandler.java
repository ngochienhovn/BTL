package com.ltnc.auction.server.server;

import com.ltnc.auction.server.protocol.ClientToServerMessage;
import com.ltnc.auction.server.protocol.ServerToClientMessage;
import com.ltnc.auction.server.protocol.MessageType;

import java.io.*;
import java.net.*;

import com.google.gson.Gson;

public class ClientHandler implements Runnable
{
    private final Socket socket;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        ) {
            String line;

            while ((line = reader.readLine()) != null)
            {
                System.out.println("Received message from client: " + line);

                ClientToServerMessage request = gson.fromJson(line, ClientToServerMessage.class);
                ServerToClientMessage response = handleMessage(request);
                writer.println(gson.toJson(response));
            }
        }
        catch (IOException e)
        {
            System.err.println("Error handling client: " + e.getMessage());
        }
        finally
        {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private ServerToClientMessage handleMessage(ClientToServerMessage mgs)
    {
        ServerToClientMessage response = new ServerToClientMessage();

        switch (mgs.type)
        {
            case LOGIN:
                response.type = MessageType.LOGIN_RESULT;
                response.success = true;
                break;
            case REGISTER:
                // msg.email, msg.password, msg.fullName, msg.role
                if (mgs.email == null || mgs.email.equals("abc@gmail.com"))
                {
                    response.type = MessageType.ERROR;
                    response.success = false;
                    response.error = "Email is required";
                    break;
                }

                response.type = MessageType.REGISTER_RESULT;
                response.success = true;
                response.error = "Register successful with email: " + mgs.email;
                break;
            case AUCTION_BID:
                response.type = MessageType.AUCTION_BID_RESULT;
                response.success = true;
                break;
            case GET_ITEMS:
                response.type = MessageType.GET_ITEMS_RESULT;
                response.success = true;
                break;
            case CREATE_AUCTION:
                response.type = MessageType.CREATE_AUCTION_RESULT;
                response.success = true;
                break;
            default:
                response.type = MessageType.ERROR;
                response.success = false;
                response.error = "Invalid message type";
                break;
        }

        return response;
    }
}