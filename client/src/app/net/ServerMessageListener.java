package app.net;

import com.google.gson.Gson;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.util.function.Consumer;

public class ServerMessageListener implements Runnable {
    private final BufferedReader reader;
    private final Consumer<ServerToClientMessage> onMessage;
    private final Gson gson = new Gson();

    public ServerMessageListener(BufferedReader reader, Consumer<ServerToClientMessage> onMessage) {
        this.reader = reader;
        this.onMessage = onMessage;
    }

    @Override
    public void run() {
        try {
            String line;

            while ((line = reader.readLine()) != null) {
                ServerToClientMessage msg = gson.fromJson(line, ServerToClientMessage.class);

                Platform.runLater(() -> {
                    if (onMessage != null) {
                        onMessage.accept(msg);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}