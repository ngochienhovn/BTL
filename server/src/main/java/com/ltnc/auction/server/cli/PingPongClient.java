package com.ltnc.auction.server.cli;

import com.google.gson.Gson;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * CLI test đơn giản cho protocol PING - PONG.
 *
 * Usage:
 *   java ... PingPongClient <host> <port>
 */
public class PingPongClient {
  public static void main(String[] args) throws Exception {
    String host = "127.0.0.1";
    int port = 5555;

    if (args.length >= 1) {
      host = args[0];
    }
    if (args.length >= 2) {
      port = Integer.parseInt(args[1]);
    }

    Gson gson = new Gson();

    try (Socket socket = new Socket(host, port)) {
      socket.setSoTimeout(5000);

      PrintWriter writer =
          new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

      ClientToServerMessage ping = new ClientToServerMessage();
      ping.type = MessageType.PING;

      writer.println(gson.toJson(ping));

      String line = reader.readLine();
      if (line == null) {
        throw new RuntimeException("No response from server");
      }

      ServerToClientMessage resp = gson.fromJson(line, ServerToClientMessage.class);
      System.out.println(resp != null ? ("Response type=" + resp.type + ", error=" + resp.error) : "No response");
    }
  }
}

