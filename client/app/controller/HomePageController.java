package app.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

public class HomePageController {
    @FXML
    private FlowPane auctionGrid;

    @FXML
    private Label resultLabel;

    private final List<AuctionCard> auctions = new ArrayList<>();

    @FXML
    public void initialize() {
        loadAuctions();
        renderAuctions();
    }

    @FXML
    private void onOpenWatchlist() {}

    @FXML
    private void onOpenSellerItems() {}

    @FXML
    private void onOpenAdminDashboard() {}

    @FXML
    private void onOpenProfile() {}

    @FXML
    private void onLogout() {}

    @FXML
    private void onOpenSignIn() {}

    @FXML
    private void onToggleCategories() {}

    @FXML
    private void onRoleDashboardAction() {}

    private void loadAuctions() {
        auctions.clear();
        auctions.add(new AuctionCard(1L, "Vintage Watch", 1200.0, "RUNNING"));
        auctions.add(new AuctionCard(2L, "Canvas Painting", 850.0, "RUNNING"));
        auctions.add(new AuctionCard(3L, "Retro Camera", 500.0, "OPEN"));
    }

    private void renderAuctions() {
        if (resultLabel != null) {
            resultLabel.setText(auctions.size() + " auctions found");
        }
        if (auctionGrid == null) {
            return;
        }
        auctionGrid.getChildren().clear();
        for (AuctionCard auction : auctions) {
            Label card = new Label(auction.title + "\nCurrent: " + auction.currentBid + "\nStatus: " + auction.status);
            card.setStyle("-fx-padding: 12; -fx-border-color: #cccccc; -fx-background-color: #ffffff;");
            card.setOnMouseClicked(e -> openAuctionDetail(auction));
            auctionGrid.getChildren().add(card);
        }
    }

    private void openAuctionDetail(AuctionCard auction) {
        // Placeholder for Sprint 3 flow: open auction-detail.fxml and pass selected auction.
    }

    private record AuctionCard(Long id, String title, Double currentBid, String status) {}
}
