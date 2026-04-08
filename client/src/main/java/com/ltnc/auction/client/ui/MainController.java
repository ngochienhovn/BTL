package com.ltnc.auction.client.ui;

import com.google.gson.Gson;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {
  private final Gson gson = new Gson();

  // Update realtime later: host/port -> config
  private final String host = "127.0.0.1";
  private final int port = 5555;

  @FXML private Label statusLabel;
  @FXML private Button pingButton;

  @FXML
  public void onPingClicked() {
    pingButton.setDisable(true);
    statusLabel.setText("Đang gửi Ping...");

    Thread t =
        new Thread(
            () -> {
              try (Socket socket = new Socket(host, port)) {
                socket.setSoTimeout(5000);

                PrintWriter writer =
                    new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                ClientToServerMessage msg = new ClientToServerMessage();
                msg.type = MessageType.PING;

                writer.println(gson.toJson(msg));

                String line = reader.readLine();
                if (line == null) {
                  throw new RuntimeException("No response from server");
                }

                ServerToClientMessage resp = gson.fromJson(line, ServerToClientMessage.class);
                Platform.runLater(() -> updateStatus(resp));
              } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Loi: " + e.getMessage()));
              } finally {
                Platform.runLater(() -> pingButton.setDisable(false));
              }
            });
    t.setDaemon(true);
    t.start();
  }

  private void updateStatus(ServerToClientMessage resp) {
    if (resp == null) {
      statusLabel.setText("Không có phản hồi");
      return;
    }
    if (resp.type == MessageType.PONG) {
      statusLabel.setText("Nhận: PONG");
      return;
    }
    if (resp.type == MessageType.ERROR) {
      statusLabel.setText("Error: " + resp.error);
      return;
    }
    statusLabel.setText("Nhận: " + resp.type);
  }
}

