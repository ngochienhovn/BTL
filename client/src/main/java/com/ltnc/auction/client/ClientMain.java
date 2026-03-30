package com.ltnc.auction.client;

import com.ltnc.auction.client.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader =
        new FXMLLoader(getClass().getResource("/com/ltnc/auction/client/ui/main.fxml"));
    Parent root = loader.load();

    // Ensure controller class is wired (main.fxml already declares fx:controller).
    if (loader.getController() instanceof MainController) {
      // no-op
    }

    Scene scene = new Scene(root, 520, 260);
    stage.setTitle("Auction Client (JavaFX + Socket)");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}

