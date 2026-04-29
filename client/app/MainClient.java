package client.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainClient extends Application {
    private static final String START_VIEW = "sign-in.fxml";

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(loadView(START_VIEW), 1000, 700);
        primaryStage.setTitle("Auction Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private javafx.scene.Parent loadView(String viewFile) 
    {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("/" + viewFile));
            if (loader.getLocation() == null) {
                return new StackPane(new Label("Khong tim thay file: " + viewFile));
            }
            ScrollPane scroll = new ScrollPane(loader.load());
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            return scroll;
        } catch (Exception ex) {
            Label error = new Label("Khong the load view " + viewFile + "\n" + ex.getMessage());
            error.setWrapText(true);
            return new StackPane(error);
        }
    }
}
