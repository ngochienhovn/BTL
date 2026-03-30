package com.ltnc.auction.server.network;

import com.google.gson.Gson;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket server (TCP) dùng "line-delimited JSON".
 * Mỗi client gửi 1 JSON/1 dòng; server trả JSON/1 dòng.
 */
public class SocketServer {
  private final int port;
  private final Gson gson = new Gson();
  private final ExecutorService executor = Executors.newFixedThreadPool(16);

  public SocketServer(int port) {
    this.port = port;
  }

  public void start() {
    System.out.println("[server] Starting on port " + port);
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      while (true) {
        Socket socket = serverSocket.accept();
        executor.submit(() -> handleClient(socket));
      }
    } catch (IOException e) {
      throw new RuntimeException("Server failed to start", e);
    }
  }

  private void handleClient(Socket socket) {
    String remote = socket.getRemoteSocketAddress() != null ? socket.getRemoteSocketAddress().toString() : "unknown";
    System.out.println("[server] Client connected: " + remote);

    try (Socket s = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }

        ClientToServerMessage request;
        try {
          request = gson.fromJson(line, ClientToServerMessage.class);
        } catch (Exception ex) {
          ServerToClientMessage err = new ServerToClientMessage();
          err.type = MessageType.ERROR;
          err.error = "Invalid JSON: " + ex.getMessage();
          writer.println(gson.toJson(err));
          continue;
        }

        ServerToClientMessage response = new ServerToClientMessage();
        if (request == null || request.type == null) {
          response.type = MessageType.ERROR;
          response.error = "Missing message type";
          writer.println(gson.toJson(response));
          continue;
        }

        switch (request.type) {
          case PING -> {
            response.type = MessageType.PONG;
            response.error = null;
          }
          default -> {
            response.type = MessageType.ERROR;
            response.error = "Not implemented for type=" + request.type;
          }
        }

        writer.println(gson.toJson(response));
      }
    } catch (IOException e) {
      System.out.println("[server] Client disconnected: " + remote);
    } finally {
      // socket/resource auto-closed by try-with-resources
    }
  }
}

