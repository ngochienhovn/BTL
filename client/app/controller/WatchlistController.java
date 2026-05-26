package app.controller;

import app.MainApp;
import app.ThemeManager;
import app.model.Auction;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class WatchlistController {
    @FXML
    private FlowPane watchlistGrid;

    @FXML
    private Label countLabel;

    private List<Auction> currentLiked;

    public void loadWatchlist(List<Auction> likedAuctions) {
        currentLiked = likedAuctions;
        watchlistGrid.getChildren().clear();
        countLabel.setText(MessageFormat.format(
                likedAuctions.size() == 1 ? ThemeManager.get("watchlist.count.one") : ThemeManager.get("watchlist.count"),
                likedAuctions.size()));

        for (Auction auction : likedAuctions) {
            watchlistGrid.getChildren().add(createCard(auction));
        }
    }

    @FXML
    private void onBackHome() {
        MainApp.showHomePage();
    }

    private VBox createCard(Auction auction) {
        VBox card = new VBox(8);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(280);

        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("card-image-wrap");
        imageWrap.setOnMouseClicked(event -> MainApp.showAuctionDetail(auction));

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 180);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        imageWrap.setClip(clip);

        ImageView imageView = new ImageView(new Image(auction.getImage(), 280, 180, false, true, true));
        imageView.setFitWidth(280);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("card-image");

        Label badge = new Label(auction.getCategory());
        badge.getStyleClass().add("card-badge");
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(16, 0, 0, 16));
        imageWrap.getChildren().addAll(imageView, badge);

        VBox body = new VBox(8);
        body.setPadding(new Insets(14));
        body.setOnMouseClicked(event -> MainApp.showAuctionDetail(auction));

        Label title = new Label(auction.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setMaxHeight(40);

        Label bid = new Label(formatCurrency(auction.getCurrentBid()));
        bid.getStyleClass().add("card-bid");

        Label bidCount = new Label(auction.getBids().size() + " bids");
        bidCount.getStyleClass().add("card-meta");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button viewDetailButton = new Button("View Detail");
        viewDetailButton.getStyleClass().add("primary-button");
        viewDetailButton.setMaxWidth(Double.MAX_VALUE);
        viewDetailButton.setOnAction(event -> MainApp.showAuctionDetail(auction));

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("secondary-button");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setOnAction(event -> {
            MainApp.toggleLiked(auction.getId());
            loadWatchlist(MainApp.getLikedAuctions());
        });

        body.getChildren().addAll(title, bid, bidCount, spacer, viewDetailButton, removeButton);
        card.getChildren().addAll(imageWrap, body);
        return card;
    }

    private String formatCurrency(double amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return numberFormat.format(amount);
    }
}

