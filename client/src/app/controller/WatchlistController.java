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

        if (likedAuctions.isEmpty()) {
            VBox emptyGraphic = new VBox(20);
            emptyGraphic.setAlignment(javafx.geometry.Pos.CENTER);
            emptyGraphic.setPadding(new Insets(80, 0, 80, 0));
            emptyGraphic.setPrefWidth(800);
            
            javafx.scene.shape.SVGPath emptyIcon = new javafx.scene.shape.SVGPath();
            emptyIcon.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
            emptyIcon.setFill(javafx.scene.paint.Color.valueOf("#94a3b8"));
            emptyIcon.setScaleX(3.0);
            emptyIcon.setScaleY(3.0);
            
            Label msg = new Label("Your watchlist is empty.");
            msg.setStyle("-fx-font-size: 18px; -fx-text-fill: -color-fg-subtle; -fx-font-weight: bold;");
            
            Button exploreBtn = new Button("Explore Auctions");
            exploreBtn.getStyleClass().add("primary-button");
            exploreBtn.setOnAction(e -> MainApp.showHomePage());
            
            emptyGraphic.getChildren().addAll(emptyIcon, new Label(), msg, exploreBtn);
            watchlistGrid.getChildren().add(emptyGraphic);
        } else {
            for (Auction auction : likedAuctions) {
                watchlistGrid.getChildren().add(createCard(auction));
            }
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

        ImageView imageView = new ImageView();
        app.util.ImageLoader.loadImage(imageView, auction.getImage(), 280, 180, false);
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
