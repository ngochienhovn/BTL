package app.controller;

import app.MainApp;
import app.model.Item;
import app.model.User;
import app.service.IAuctionService;
import app.service.IItemService;
import app.service.ItemObserver;
import app.service.NetworkItemService;
import app.service.NotificationService;
import app.util.ImageLoader;
import java.time.LocalDateTime;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Insets;


public class SellerItemsController implements ItemObserver {
    @FXML
    private ListView<Item> itemListView;

    @FXML
    private ComboBox<String> typeCombo;

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField startingBidField;

    @FXML
    private TextField imageUrlField;

    @FXML
    private TextField sessionStartField;

    @FXML
    private TextField sessionEndField;

    @FXML
    private Label totalItemsLabel;
    @FXML
    private Label activeAuctionsLabel;
    @FXML
    private Label soldItemsLabel;
    @FXML
    private Label itemCountSubLabel;

    @FXML
    private Label messageLabel;

    private final IItemService itemService = MainApp.getItemService();
    private final IAuctionService auctionService = MainApp.getAuctionService();
    private User seller;
    private final app.service.AuctionObserver auctionObserver = this::refreshItems;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll("Electronics", "Art", "Vehicle", "Watches", "Jewelry", "Furniture", "Collectibles");
        typeCombo.setValue("Electronics");
        
        itemListView.setCellFactory(list -> new ItemListCell());
        itemListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> bindForm(selected));
        
        Label emptyStateLabel = new Label("You haven't listed any items yet.");
        SVGPath emptyIcon = new SVGPath();
        emptyIcon.setContent("M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
        emptyIcon.setFill(javafx.scene.paint.Color.valueOf("#94a3b8"));
        emptyIcon.setScaleX(2.0);
        emptyIcon.setScaleY(2.0);
        
        VBox emptyGraphic = new VBox(16);
        emptyGraphic.setAlignment(javafx.geometry.Pos.CENTER);
        emptyGraphic.setPadding(new Insets(20, 0, 20, 0));
        emptyGraphic.getChildren().addAll(emptyIcon, emptyStateLabel);
        emptyStateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -color-fg-subtle; -fx-font-weight: bold;");
        itemListView.setPlaceholder(emptyGraphic);
        
        auctionService.registerObserver(auctionObserver);
        NetworkItemService.getInstance().registerObserver(this);
    }

    public void cleanup() {
        auctionService.unregisterObserver(auctionObserver);
        NetworkItemService.getInstance().unregisterObserver(this);
    }

    @Override
    public void onItemsUpdated() {
        Platform.runLater(this::refreshItems);
    }

    private class ItemListCell extends javafx.scene.control.ListCell<Item> {
        private final HBox root = new HBox(16);
        private final ImageView imgView = new ImageView();
        private final VBox infoBox = new VBox(4);
        private final Label nameLabel = new Label();
        private final Label typeLabel = new Label();
        private final Label priceLabel = new Label();
        private final Region spacer = new Region();

        public ItemListCell() {
            root.getStyleClass().add("item-row");
            nameLabel.getStyleClass().add("item-row-title");
            typeLabel.getStyleClass().add("item-row-meta");
            priceLabel.getStyleClass().add("item-row-price");
            
            imgView.setFitHeight(60);
            imgView.setFitWidth(80);
            imgView.setPreserveRatio(true);
            imgView.getStyleClass().add("item-row-image");
            
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            infoBox.getChildren().addAll(nameLabel, typeLabel);
            root.getChildren().addAll(imgView, infoBox, spacer, priceLabel);
            root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Item item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                typeLabel.setText(item.getType());
                priceLabel.setText("$" + String.format("%.2f", item.getStartingBid()));
                
                ImageLoader.loadImage(imgView, item.getImageUrl(), 80, 60, true);
                setGraphic(root);
            }
        }
    }

    public void loadSellerItems(User currentUser) {
        this.seller = currentUser;
        if (seller == null) return;

        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            try {
                // Fetch from network to populate cache
                itemService.getItemsBySeller(seller.getEmail());
                Platform.runLater(this::refreshItems);
            } finally {
                app.service.LoadingService.getInstance().hide();
            }
        }).start();
    }

    private boolean validateItemFields() {
        if (typeCombo.getValue() == null || typeCombo.getValue().isBlank()) {
            showValidationError("Category is required.");
            return false;
        }
        if (nameField.getText() == null || nameField.getText().trim().isBlank()) {
            showValidationError("Item Name is required.");
            return false;
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isBlank()) {
            showValidationError("Description is required.");
            return false;
        }
        double bid = parseDouble(startingBidField.getText());
        if (bid <= 0) {
            showValidationError("Starting Bid must be greater than 0.");
            return false;
        }
        return true;
    }

    private void showValidationError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
        NotificationService.getInstance().showNotification(
            "Validation Error",
            msg,
            NotificationService.NotificationType.ERROR
        );
    }

    @FXML
    private void onAddItem() {
        if (seller == null) return;
        if (!validateItemFields()) return;
        
        String type = typeCombo.getValue();
        String name = safe(nameField.getText());
        String description = safe(descriptionArea.getText());
        double startingBid = parseDouble(startingBidField.getText());
        String imageUrl = safe(imageUrlField.getText());
        String email = seller.getEmail();

        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            Item item = itemService.createItem(type, name, description, startingBid, imageUrl, email);
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (item != null) {
                    messageLabel.setText("Added item: " + item.getName());
                    messageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
                    NotificationService.getInstance().showNotification(
                        "Success", "Added item: " + item.getName(), NotificationService.NotificationType.SUCCESS
                    );
                    refreshItems();
                    for (Item it : itemListView.getItems()) {
                        if (item.getId().equals(it.getId())) {
                            itemListView.getSelectionModel().select(it);
                            break;
                        }
                    }
                } else {
                    showValidationError("Failed to add item. The server rejected the request.");
                    NotificationService.getInstance().showNotification(
                        "Error", "Failed to add item. Check server logs.", NotificationService.NotificationType.ERROR
                    );
                    refreshItems();
                }
            });
        }).start();
    }

    @FXML
    private void onUpdateItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationService.getInstance().showNotification("Error", "Please select an item to update.", NotificationService.NotificationType.ERROR);
            return;
        }
        if (!validateItemFields()) return;
        
        String name = safe(nameField.getText());
        String description = safe(descriptionArea.getText());
        double startingBid = parseDouble(startingBidField.getText());
        String imageUrl = safe(imageUrlField.getText());

        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            boolean ok = itemService.updateItem(selected, name, description, startingBid, imageUrl);
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (ok) {
                    messageLabel.setText("Updated item.");
                    messageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
                    NotificationService.getInstance().showNotification("Success", "Updated item: " + selected.getName(), NotificationService.NotificationType.SUCCESS);
                } else {
                    showValidationError("Failed to update item.");
                    NotificationService.getInstance().showNotification("Error", "Failed to update item. It might be locked.", NotificationService.NotificationType.ERROR);
                }
                refreshItems();
            });
        }).start();
    }

    @FXML
    private void onDeleteItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationService.getInstance().showNotification("Error", "Please select an item to delete.", NotificationService.NotificationType.ERROR);
            return;
        }

        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            boolean ok = itemService.deleteItem(selected.getId());
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (ok) {
                    messageLabel.setText("Deleted item.");
                    messageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
                    NotificationService.getInstance().showNotification("Success", "Deleted item.", NotificationService.NotificationType.SUCCESS);
                } else {
                    showValidationError("Failed to delete item.");
                    NotificationService.getInstance().showNotification("Error", "Failed to delete item. It might be locked or already removed.", NotificationService.NotificationType.ERROR);
                }
                refreshItems();
            });
        }).start();
    }

    private LocalDateTime parseDateTime(String text, LocalDateTime defaultValue) {
        if (text == null || text.trim().isBlank()) {
            return defaultValue;
        }
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(text.trim(), formatter);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void onCreateSession() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationService.getInstance().showNotification(
                "Error",
                "Please select an item to start a session.",
                NotificationService.NotificationType.ERROR
            );
            return;
        }

        String startStr = sessionStartField.getText().trim();
        String endStr = sessionEndField.getText().trim();

        LocalDateTime startTime = LocalDateTime.now();
        if (!startStr.isEmpty()) {
            startTime = parseDateTime(startStr, null);
            if (startTime == null) {
                showValidationError("Invalid Start Time format. Use yyyy-MM-dd HH:mm");
                return;
            }
        }

        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        if (!endStr.isEmpty()) {
            endTime = parseDateTime(endStr, null);
            if (endTime == null) {
                showValidationError("Invalid End Time format. Use yyyy-MM-dd HH:mm");
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now.minusMinutes(5))) {
            showValidationError("Start Time cannot be in the past.");
            return;
        }

        if (endTime.isBefore(startTime)) {
            showValidationError("End Time must be after Start Time.");
            return;
        }

        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < 30) {
            showValidationError("Auction duration must be at least 30 minutes.");
            return;
        }

        app.service.LoadingService.getInstance().show();
        auctionService.createAuctionFromItem(selected, startTime, endTime);
        app.service.LoadingService.getInstance().hide();
        messageLabel.setText("Auction session created.");
        messageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
        NotificationService.getInstance().showNotification(
            "Success",
            "Auction session created successfully.",
            NotificationService.NotificationType.SUCCESS
        );
    }

    @FXML
    private void onBackHome() {
        MainApp.showHomePage();
    }

    private void bindForm(Item selected) {
        if (selected == null) {
            return;
        }
        typeCombo.setValue(selected.getType());
        nameField.setText(selected.getName());
        descriptionArea.setText(selected.getDescription());
        startingBidField.setText(String.valueOf(selected.getStartingBid()));
        imageUrlField.setText(selected.getImageUrl());
    }

    private void refreshItems() {
        if (seller == null) {
            itemListView.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Item> items = itemService.getCachedItemsBySeller(seller.getEmail());
        
        // Save the currently selected item's ID
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        String selectedId = selected != null ? selected.getId() : null;
        
        itemListView.setItems(FXCollections.observableArrayList(items));
        
        // Restore the selection if it was found
        if (selectedId != null) {
            for (Item item : items) {
                if (selectedId.equals(item.getId())) {
                    itemListView.getSelectionModel().select(item);
                    break;
                }
            }
        }
        
        // Update stats
        totalItemsLabel.setText(String.valueOf(items.size()));
        itemCountSubLabel.setText(items.size() + " items found");
        
        // Calculate active and sold (simulated or filter from sessions)
        long active = auctionService.getCachedAuctions().stream()
                .filter(a -> items.stream().anyMatch(i -> i.getId().equals(a.getItemId())))
                .filter(a -> a.getStatus() == app.model.AuctionStatus.OPEN || a.getStatus() == app.model.AuctionStatus.RUNNING)
                .count();
        activeAuctionsLabel.setText(String.valueOf(active));
        
        long sold = auctionService.getCachedAuctions().stream()
                .filter(a -> items.stream().anyMatch(i -> i.getId().equals(a.getItemId())))
                .filter(a -> a.getStatus() == app.model.AuctionStatus.FINISHED || a.getStatus() == app.model.AuctionStatus.PAID)
                .count();
        soldItemsLabel.setText(String.valueOf(sold));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
