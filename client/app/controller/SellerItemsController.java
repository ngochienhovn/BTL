package client.app.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SellerItemsController 
{
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
    private Label messageLabel;

    private final IItemService itemService = InMemoryItemService.INSTANCE;
    private final IAuctionService auctionService = DummyAuctionService.INSTANCE;
    private User seller;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll("Electronics", "Art", "Vehicle");
        typeCombo.setValue("Electronics");
        itemListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getType() + ")");
            }
        });
        itemListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> bindForm(selected));
    }

    public void loadSellerItems(User currentUser) {
        this.seller = currentUser;
        refreshItems();
    }

    @FXML
    private void onAddItem() {
        if (seller == null) {
            return;
        }
        Item item = itemService.createItem(
                typeCombo.getValue(),
                safe(nameField.getText()),
                safe(descriptionArea.getText()),
                parseDouble(startingBidField.getText()),
                safe(imageUrlField.getText()),
                seller.getEmail());
        messageLabel.setText("Added item: " + item.getName());
        refreshItems();
    }

    @FXML
    private void onUpdateItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        itemService.updateItem(selected, safe(nameField.getText()), safe(descriptionArea.getText()),
                parseDouble(startingBidField.getText()), safe(imageUrlField.getText()));
        messageLabel.setText("Updated item.");
        refreshItems();
    }

    @FXML
    private void onDeleteItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        itemService.deleteItem(selected.getId());
        messageLabel.setText("Deleted item.");
        refreshItems();
    }

    @FXML
    private void onCreateSession() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        auctionService.createAuctionFromItem(selected, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        messageLabel.setText("Auction session created from item.");
    }

    @FXML
    private void onBackHome() {
        messageLabel.setText("Back Home (demo).");
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
        List<Item> items = itemService.getItemsBySeller(seller.getEmail());
        itemListView.setItems(FXCollections.observableArrayList(items));
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

    static final class User {
        private final String email;

        User(String email) {
            this.email = email == null ? "" : email.trim().toLowerCase();
        }

        String getEmail() {
            return email;
        }
    }

    static final class Item {
        private final String id;
        private String type;
        private String name;
        private String description;
        private double startingBid;
        private String imageUrl;
        private final String sellerEmail;

        Item(String type, String name, String description, double startingBid, String imageUrl, String sellerEmail) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.name = name;
            this.description = description;
            this.startingBid = startingBid;
            this.imageUrl = imageUrl;
            this.sellerEmail = sellerEmail;
        }

        String getId() { return id; }
        String getType() { return type; }
        String getName() { return name; }
        String getDescription() { return description; }
        double getStartingBid() { return startingBid; }
        String getImageUrl() { return imageUrl; }
        String getSellerEmail() { return sellerEmail; }
    }

    interface IItemService {
        Item createItem(String type, String name, String description, double startingBid, String imageUrl, String sellerEmail);
        void updateItem(Item item, String name, String description, double startingBid, String imageUrl);
        void deleteItem(String id);
        List<Item> getItemsBySeller(String sellerEmail);
    }

    interface IAuctionService {
        void createAuctionFromItem(Item item, LocalDateTime startAt, LocalDateTime endAt);
    }

    static final class InMemoryItemService implements IItemService {
        private static final InMemoryItemService INSTANCE = new InMemoryItemService();
        private final List<Item> items = new ArrayList<>();

        private InMemoryItemService() {}

        @Override
        public Item createItem(String type, String name, String description, double startingBid, String imageUrl, String sellerEmail) {
            Item item = new Item(type, name, description, startingBid, imageUrl, sellerEmail);
            items.add(item);
            return item;
        }

        @Override
        public void updateItem(Item item, String name, String description, double startingBid, String imageUrl) {
            item.name = name;
            item.description = description;
            item.startingBid = startingBid;
            item.imageUrl = imageUrl;
        }

        @Override
        public void deleteItem(String id) {
            items.removeIf(i -> i.getId().equals(id));
        }

        @Override
        public List<Item> getItemsBySeller(String sellerEmail) {
            if (sellerEmail == null || sellerEmail.isBlank()) {
                return Collections.emptyList();
            }
            List<Item> result = new ArrayList<>();
            for (Item item : items) {
                if (sellerEmail.equalsIgnoreCase(item.getSellerEmail())) {
                    result.add(item);
                }
            }
            return result;
        }
    }

    static final class DummyAuctionService implements IAuctionService {
        private static final DummyAuctionService INSTANCE = new DummyAuctionService();
        private DummyAuctionService() {}

        @Override
        public void createAuctionFromItem(Item item, LocalDateTime startAt, LocalDateTime endAt) {
            // no-op for UI demo mode
        }
    }
}