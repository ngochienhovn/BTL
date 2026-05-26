package app.controller;

import app.MainApp;
import app.model.Auction;
import app.model.Item;
import app.model.User;
import app.model.UserRole;
import app.service.IAuctionService;
import app.service.IItemService;
import app.service.IUserService;
import app.service.ItemObserver;
import app.service.NetworkItemService;
import app.service.NetworkUserService;
import app.service.UserObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

public class AdminDashboardController implements ItemObserver, UserObserver {
    @FXML
    private ListView<Auction> auctionListView;

    @FXML
    private ListView<User> userListView;

    @FXML
    private ListView<String> bidLogListView;

    @FXML
    private TextField userNameField;

    @FXML
    private TextField userPasswordField;

    @FXML
    private ComboBox<String> userRoleCombo;

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalAuctionsLabel;
    
    @FXML
    private Label runningCountLabel;
    @FXML
    private Label finishedCountLabel;
    @FXML
    private Label canceledCountLabel;
    @FXML
    private Label paidCountLabel;

    @FXML
    private TextField userSearchField;
    @FXML
    private ComboBox<String> userRoleFilterCombo;

    @FXML
    private TextField auctionSearchField;
    @FXML
    private ComboBox<String> auctionStatusFilterCombo;

    @FXML
    private PieChart categoryChart;

    @FXML
    private Label adminMessageLabel;

    private final IItemService itemService = MainApp.getItemService();
    private final IUserService userService = MainApp.getUserService();
    private final IAuctionService auctionService = MainApp.getAuctionService();

    private List<User> rawUsers = FXCollections.observableArrayList();
    private List<Auction> rawAuctions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        auctionListView.setCellFactory(list -> new ListCell<Auction>() {
            @Override
            protected void updateItem(Auction a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setText(null);
                } else {
                    setText(String.format("[%s] %s | Current: $%.2f | Seller: %s", 
                        a.getStatus(), a.getTitle(), a.getCurrentBid(), a.getSeller()));
                }
            }
        });

        userListView.setCellFactory(list -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFullName() + " (" + user.getRole().name() + ") - " + user.getEmail());
                }
            }
        });

        userRoleCombo.getItems().setAll("BIDDER", "SELLER", "ADMIN");
        userRoleCombo.setValue("BIDDER");
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> bindUserForm(selected));

        userRoleFilterCombo.getItems().setAll("All", "BIDDER", "SELLER", "ADMIN");
        userRoleFilterCombo.setValue("All");
        userRoleFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
        
        if (userSearchField != null) {
            userSearchField.textProperty().addListener((obs, old, val) -> applyFilters());
        }

        auctionStatusFilterCombo.getItems().setAll("All", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
        auctionStatusFilterCombo.setValue("All");
        auctionStatusFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());

        if (auctionSearchField != null) {
            auctionSearchField.textProperty().addListener((obs, old, val) -> applyFilters());
        }
        
        auctionService.registerObserver(this::refreshAll);
        NetworkItemService.getInstance().registerObserver(this);
        NetworkUserService.getInstance().registerObserver(this);
        refreshAll();
    }

    public void cleanup() {
        auctionService.unregisterObserver(this::refreshAll);
        NetworkItemService.getInstance().unregisterObserver(this);
        NetworkUserService.getInstance().unregisterObserver(this);
    }

    @Override
    public void onItemsUpdated() {
        Platform.runLater(this::refreshAll);
    }

    @Override
    public void onUsersUpdated() {
        Platform.runLater(this::refreshAll);
    }

    public void loadAuctions(List<Auction> auctions) {
        refreshAll();
    }

    private boolean showConfirmDialog(String header, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Action");
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK;
    }

    @FXML
    private void onConfirmPayment() {
        Auction selected = auctionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select an auction to confirm payment.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (selected.getStatus() != app.model.AuctionStatus.FINISHED) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Only FINISHED auctions can be confirmed as PAID.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Confirm Payment?", "Are you sure you want to mark auction: " + selected.getTitle() + " as PAID?")) {
            auctionService.confirmPayment(selected.getId());
            app.service.NotificationService.getInstance().showNotification(
                "Success",
                "Confirmed payment for: " + selected.getTitle(),
                app.service.NotificationService.NotificationType.SUCCESS
            );
            refreshAll();
        }
    }

    @FXML
    private void onEditAuction() {
        Auction selected = auctionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select an auction to edit.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }

        // Custom Dialog for Editing
        javafx.scene.control.Dialog<Auction> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Edit Auction");
        dialog.setHeaderText("Edit details for: " + selected.getTitle());

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField title = new TextField(selected.getTitle());
        TextField description = new TextField(selected.getDescription());
        TextField startingBid = new TextField(String.valueOf(selected.getStartingBid()));
        TextField endTime = new TextField(selected.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(description, 1, 1);
        grid.add(new Label("Starting Bid:"), 0, 2);
        grid.add(startingBid, 1, 2);
        grid.add(new Label("End Time (yyyy-MM-dd HH:mm):"), 0, 3);
        grid.add(endTime, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double bid = Double.parseDouble(startingBid.getText());
                    java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime.getText(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    auctionService.updateAuction(selected.getId(), title.getText(), description.getText(), bid, end);
                    return selected;
                } catch (Exception e) {
                    app.service.NotificationService.getInstance().showNotification("Error", "Invalid input format.", app.service.NotificationService.NotificationType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            app.service.NotificationService.getInstance().showNotification(
                "Success",
                "Auction updated: " + selected.getTitle(),
                app.service.NotificationService.NotificationType.SUCCESS
            );
            refreshAll();
        });
    }
 
    @FXML
    private void onDeleteAuction() {
        Auction selected = auctionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select an auction to delete.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Delete Auction?", "Are you sure you want to delete auction: " + selected.getTitle() + "?\nAll bids and reserved balances will be released.")) {
            auctionService.deleteAuction(selected.getId());
            app.service.NotificationService.getInstance().showNotification(
                "Success",
                "Deleted auction: " + selected.getTitle(),
                app.service.NotificationService.NotificationType.SUCCESS
            );
            refreshAll();
        }
    }

    @FXML
    private void onCancelAuction() {
        Auction selected = auctionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select an auction to cancel.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Cancel Auction?", "Are you sure you want to cancel auction: " + selected.getTitle() + "?\nThis will set the status to CANCELED and release all reserve funds.")) {
            auctionService.cancelAuction(selected.getId());
            app.service.NotificationService.getInstance().showNotification(
                "Success",
                "Cancelled auction: " + selected.getTitle(),
                app.service.NotificationService.NotificationType.SUCCESS
            );
            refreshAll();
        }
    }

    @FXML
    private void onDeleteSelectedUser() {
        User selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select a user to delete.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Delete User?", "Are you sure you want to delete user: " + selected.getEmail() + "?")) {
            boolean ok = userService.deleteUser(selected.getEmail());
            if (ok) {
                app.service.NotificationService.getInstance().showNotification(
                    "Success",
                    "Deleted user: " + selected.getEmail(),
                    app.service.NotificationService.NotificationType.SUCCESS
                );
            } else {
                app.service.NotificationService.getInstance().showNotification(
                    "Error",
                    "Cannot delete user.",
                    app.service.NotificationService.NotificationType.ERROR
                );
            }
            refreshAll();
        }
    }

    @FXML
    private void onUpdateSelectedUser() {
        User selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "Please select a user to update.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Update User?", "Are you sure you want to update user: " + selected.getEmail() + "?")) {
            UserRole role = UserRole.valueOf(userRoleCombo.getValue());
            boolean ok = userService.updateUser(selected.getEmail(), userNameField.getText(), userPasswordField.getText(), role);
            if (ok) {
                app.service.NotificationService.getInstance().showNotification(
                    "Success",
                    "Updated user: " + selected.getEmail(),
                    app.service.NotificationService.NotificationType.SUCCESS
                );
            } else {
                app.service.NotificationService.getInstance().showNotification(
                    "Error",
                    "Cannot update user.",
                    app.service.NotificationService.NotificationType.ERROR
                );
            }
            refreshAll();
        }
    }

    private void bindUserForm(User selected) {
        if (selected == null) {
            userNameField.clear();
            userPasswordField.clear();
            userRoleCombo.setValue("BIDDER");
            return;
        }
        userNameField.setText(selected.getFullName());
        userPasswordField.clear();
        userRoleCombo.setValue(selected.getRole().name());
    }

    private void refreshAll() {
        rawUsers = userService.getAllUsersList();
        rawAuctions = auctionService.getAdminViewAuctions();
        
        // Stats Update
        totalUsersLabel.setText(String.valueOf(rawUsers.size()));
        totalAuctionsLabel.setText(String.valueOf(rawAuctions.size()));

        long running = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.RUNNING).count();
        long finished = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.FINISHED).count();
        long canceled = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.CANCELED).count();
        long paid = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.PAID).count();

        runningCountLabel.setText(String.valueOf(running));
        finishedCountLabel.setText(String.valueOf(finished));
        canceledCountLabel.setText(String.valueOf(canceled));
        paidCountLabel.setText(String.valueOf(paid));

        applyFilters();

        List<app.service.BidLogEntry> logs = auctionService.getBidLogs();
        bidLogListView.setItems(FXCollections.observableArrayList(
                logs.stream()
                        .map(log -> log.timestamp() + " | " + log.bidder() + " bid " + log.amount() + " on " + log.auctionTitle())
                        .toList()));

        // Chart Update
        Map<String, Long> categoryCounts = rawAuctions.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(Auction::getCategory, Collectors.counting()));
        
        categoryChart.getData().clear();
        categoryCounts.forEach((cat, count) -> {
            categoryChart.getData().add(new javafx.scene.chart.PieChart.Data(cat, count));
        });
    }

    private void applyFilters() {
        if (rawUsers == null || rawAuctions == null) return;

        // User Filtering
        String userQuery = userSearchField != null ? userSearchField.getText().trim().toLowerCase() : "";
        String roleFilter = userRoleFilterCombo != null ? userRoleFilterCombo.getValue() : "All";
        List<User> filteredUsers = rawUsers.stream()
            .filter(u -> u.getFullName().toLowerCase().contains(userQuery) || u.getEmail().toLowerCase().contains(userQuery))
            .filter(u -> "All".equals(roleFilter) || u.getRole().name().equals(roleFilter))
            .collect(Collectors.toList());
        userListView.setItems(FXCollections.observableArrayList(filteredUsers));

        // Auction Filtering
        String auctionQuery = auctionSearchField != null ? auctionSearchField.getText().trim().toLowerCase() : "";
        String statusFilter = auctionStatusFilterCombo != null ? auctionStatusFilterCombo.getValue() : "All";
        List<Auction> filteredAuctions = rawAuctions.stream()
            .filter(a -> a.getTitle().toLowerCase().contains(auctionQuery) || a.getSeller().toLowerCase().contains(auctionQuery))
            .filter(a -> "All".equals(statusFilter) || a.getStatus().name().equals(statusFilter))
            .collect(Collectors.toList());
        auctionListView.setItems(FXCollections.observableArrayList(filteredAuctions));
    }

    @FXML
    private void onBackHome() {
        MainApp.showHomePage();
    }
}
