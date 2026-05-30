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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.stage.FileChooser;

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
    private Label openCountLabel;
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

    @FXML private TextField itemNameField;
    @FXML private TextField itemDescriptionField;
    @FXML private TextField itemBidField;
    @FXML private TextField itemSearchField;
    @FXML private ListView<Item> itemListView;

    @FXML private Button confirmPaymentBtn;

    private final IItemService itemService = MainApp.getItemService();
    private final IUserService userService = MainApp.getUserService();
    private final IAuctionService auctionService = MainApp.getAuctionService();

    private List<User> rawUsers = FXCollections.observableArrayList();
    private List<Auction> rawAuctions = FXCollections.observableArrayList();
    private final app.service.AuctionObserver auctionObserver = this::refreshAll;

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
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                userNameField.setText(newVal.getFullName());
                userRoleCombo.setValue(newVal.getRole().name());
                userPasswordField.setText(""); 
            }
        });

        if (itemListView != null) {
            itemListView.setCellFactory(list -> new ListCell<Item>() {
                @Override
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("[%s] %s | Seller: %s | $%.2f", 
                            item.getId(), item.getName(), item.getSellerEmail(), item.getStartingBid()));
                    }
                }
            });

            itemListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    itemNameField.setText(newVal.getName());
                    itemDescriptionField.setText(newVal.getDescription());
                    itemBidField.setText(String.valueOf(newVal.getStartingBid()));
                }
            });

            itemSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshItems());
        }

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
        
        if (confirmPaymentBtn != null) {
            confirmPaymentBtn.setDisable(true);
            auctionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    confirmPaymentBtn.setDisable(true);
                } else {
                    boolean canConfirm = newVal.getStatus() == app.model.AuctionStatus.FINISHED 
                        && newVal.getWinnerBidder() != null 
                        && !newVal.getWinnerBidder().isBlank();
                    confirmPaymentBtn.setDisable(!canConfirm);
                }
            });
        }
        
        auctionService.registerObserver(auctionObserver);
        NetworkItemService.getInstance().registerObserver(this);
        NetworkUserService.getInstance().registerObserver(this);
        refreshAll();
    }

    public void cleanup() {
        auctionService.unregisterObserver(auctionObserver);
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
        if (selected.getWinnerBidder() == null || selected.getWinnerBidder().isBlank()) {
            app.service.NotificationService.getInstance().showNotification(
                "Error",
                "This auction finished without any bids (no winner). Cannot confirm payment.",
                app.service.NotificationService.NotificationType.ERROR
            );
            return;
        }
        if (showConfirmDialog("Confirm Payment?", "Are you sure you want to mark auction: " + selected.getTitle() + " as PAID?")) {
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = auctionService.confirmPayment(selected.getId());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        app.service.NotificationService.getInstance().showNotification(
                            "Success",
                            "Confirmed payment for: " + selected.getTitle(),
                            app.service.NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        app.service.NotificationService.getInstance().showNotification(
                            "Error",
                            "Failed to confirm payment. Auction might have no bids or winner has insufficient funds.",
                            app.service.NotificationService.NotificationType.ERROR
                        );
                    }
                    refreshAll();
                });
            }).start();
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
        javafx.scene.control.Dialog<Object[]> dialog = new javafx.scene.control.Dialog<>();
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
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"));
        statusCombo.setValue(selected.getStatus().name());

        grid.add(new Label("Title:"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(description, 1, 1);
        grid.add(new Label("Starting Bid:"), 0, 2);
        grid.add(startingBid, 1, 2);
        grid.add(new Label("End Time (yyyy-MM-dd HH:mm):"), 0, 3);
        grid.add(endTime, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double bid = Double.parseDouble(startingBid.getText());
                    java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime.getText(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    return new Object[]{title.getText(), description.getText(), bid, end, statusCombo.getValue()};
                } catch (Exception e) {
                    app.service.NotificationService.getInstance().showNotification("Error", "Invalid input format.", app.service.NotificationService.NotificationType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = auctionService.updateAuction(selected.getId(), (String)result[0], (String)result[1], (Double)result[2], (java.time.LocalDateTime)result[3], (String)result[4]);
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        app.service.NotificationService.getInstance().showNotification(
                            "Success",
                            "Auction updated: " + (String)result[0],
                            app.service.NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        app.service.NotificationService.getInstance().showNotification(
                            "Error",
                            "Failed to update auction. The server rejected the request.",
                            app.service.NotificationService.NotificationType.ERROR
                        );
                    }
                    refreshAll();
                });
            }).start();
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
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = auctionService.deleteAuction(selected.getId());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        app.service.NotificationService.getInstance().showNotification(
                            "Success",
                            "Deleted auction: " + selected.getTitle(),
                            app.service.NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        app.service.NotificationService.getInstance().showNotification(
                            "Error",
                            "Failed to delete auction. It might be locked or already removed.",
                            app.service.NotificationService.NotificationType.ERROR
                        );
                    }
                    refreshAll();
                });
            }).start();
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
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = auctionService.cancelAuction(selected.getId());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        app.service.NotificationService.getInstance().showNotification(
                            "Success",
                            "Cancelled auction: " + selected.getTitle(),
                            app.service.NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        app.service.NotificationService.getInstance().showNotification(
                            "Error",
                            "Failed to cancel auction. Only OPEN or RUNNING auctions can be cancelled.",
                            app.service.NotificationService.NotificationType.ERROR
                        );
                    }
                    refreshAll();
                });
            }).start();
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

        boolean isActiveSeller = rawAuctions.stream().anyMatch(a -> 
            (a.getStatus() == app.model.AuctionStatus.OPEN || a.getStatus() == app.model.AuctionStatus.RUNNING) 
            && a.getSeller().equalsIgnoreCase(selected.getEmail())
        );

        boolean isActiveBidder = rawAuctions.stream().anyMatch(a -> {
            if (a.getStatus() != app.model.AuctionStatus.OPEN && a.getStatus() != app.model.AuctionStatus.RUNNING) {
                return false;
            }
            if (a.getWinnerBidder() != null) {
                return a.getWinnerBidder().equalsIgnoreCase(selected.getEmail());
            }
            if (a.getBids() != null && !a.getBids().isEmpty()) {
                app.model.Bid maxBid = a.getBids().stream()
                        .max(java.util.Comparator.comparingDouble(app.model.Bid::getAmount))
                        .orElse(null);
                return maxBid != null && maxBid.getBidderEmail().equalsIgnoreCase(selected.getEmail());
            }
            return false;
        });

        StringBuilder warning = new StringBuilder("Are you sure you want to delete user: " + selected.getEmail() + "?");
        if (isActiveSeller || isActiveBidder) {
            warning.append("\n\nWARNING:");
            if (isActiveSeller) {
                warning.append("\n- User has active auctions as a seller.");
            }
            if (isActiveBidder) {
                warning.append("\n- User is the highest bidder on active auctions.");
            }
            warning.append("\nDeletion will fail on the server unless these sessions are cancelled first.");
        }

        if (showConfirmDialog("Delete User?", warning.toString())) {
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                String result = userService.deleteUser(selected.getEmail());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if ("OK".equals(result)) {
                        app.service.NotificationService.getInstance().showNotification(
                            "Success",
                            "Deleted user: " + selected.getEmail(),
                            app.service.NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        String errorMsg = "Cannot delete user.";
                        if ("ACTIVE_SELLER_AUCTIONS".equals(result)) {
                            errorMsg = "Cannot delete user: user is a seller on active auctions.";
                        } else if ("ACTIVE_BIDDER_ENGAGEMENTS".equals(result)) {
                            errorMsg = "Cannot delete user: user is the highest bidder on active auctions.";
                        } else if ("USER_NOT_FOUND".equals(result)) {
                            errorMsg = "Cannot delete user: user not found.";
                        }
                        app.service.NotificationService.getInstance().showNotification(
                            "Error",
                            errorMsg,
                            app.service.NotificationService.NotificationType.ERROR
                        );
                    }
                    refreshAll();
                });
            }).start();
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

        String name = userNameField.getText();
        String password = userPasswordField.getText();
        if (password != null && password.isBlank()) {
            password = null;
        }
        final String finalPassword = password;
        UserRole role = UserRole.valueOf(userRoleCombo.getValue());

        if (showConfirmDialog("Update User?", "Update " + selected.getEmail() + "?")) {
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = userService.updateUser(selected.getEmail(), name, finalPassword, role);
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
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
                });
            }).start();
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

    private void refreshItems() {
        if (itemListView == null) return;
        List<Item> allItems = itemService.getAllItems();
        String query = itemSearchField != null && itemSearchField.getText() != null ? itemSearchField.getText().toLowerCase() : "";
        
        List<Item> filtered = allItems.stream()
            .filter(i -> query.isBlank() || 
                (i.getName() != null && i.getName().toLowerCase().contains(query)) || 
                (i.getDescription() != null && i.getDescription().toLowerCase().contains(query)))
            .collect(Collectors.toList());
            
        itemListView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshAll() {
        rawUsers = userService.getAllUsersList();
        rawAuctions = auctionService.getAdminViewAuctions();
        refreshItems();
        
        // Stats Update
        totalUsersLabel.setText(String.valueOf(rawUsers.size()));
        totalAuctionsLabel.setText(String.valueOf(rawAuctions.size()));


        long open = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.OPEN).count();
        long running = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.RUNNING).count();
        long finished = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.FINISHED).count();
        long canceled = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.CANCELED).count();
        long paid = rawAuctions.stream().filter(a -> a.getStatus() == app.model.AuctionStatus.PAID).count();

        if (openCountLabel != null) {
            openCountLabel.setText(String.valueOf(open));
        }
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
                .filter(a -> a.getCategory() != null && !a.getCategory().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getCategory().trim().toUpperCase(), Collectors.counting()));
        
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
    private void onUpdateSelectedItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (adminMessageLabel != null) adminMessageLabel.setText("Please select an item to update");
            return;
        }
        
        String newName = itemNameField != null ? itemNameField.getText() : selected.getName();
        String newDesc = itemDescriptionField != null ? itemDescriptionField.getText() : selected.getDescription();
        double newBid = selected.getStartingBid();
        if (itemBidField != null && !itemBidField.getText().trim().isEmpty()) {
            try {
                newBid = Double.parseDouble(itemBidField.getText());
            } catch (NumberFormatException e) {
                if (adminMessageLabel != null) {
                    adminMessageLabel.setText("Invalid starting bid format");
                    adminMessageLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
                }
                return;
            }
        }
        
        app.service.LoadingService.getInstance().show();
        final double finalBid = newBid;
        new Thread(() -> {
            boolean ok = itemService.updateItem(selected, newName, newDesc, finalBid, selected.getImageUrl());
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (ok) {
                    if (adminMessageLabel != null) {
                        adminMessageLabel.setText("Item updated successfully");
                        adminMessageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
                    }
                } else {
                    if (adminMessageLabel != null) {
                        adminMessageLabel.setText("Failed to update item");
                        adminMessageLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
                    }
                }
            });
        }).start();
    }

    @FXML
    private void onDeleteSelectedItem() {
        Item selected = itemListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (adminMessageLabel != null) adminMessageLabel.setText("Please select an item to delete");
            return;
        }
        
        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            boolean ok = itemService.deleteItem(selected.getId());
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (ok) {
                    if (adminMessageLabel != null) {
                        adminMessageLabel.setText("Item deleted successfully");
                        adminMessageLabel.setStyle("-fx-text-fill: -color-success-emphasis;");
                    }
                    if (itemNameField != null) itemNameField.clear();
                    if (itemDescriptionField != null) itemDescriptionField.clear();
                    if (itemBidField != null) itemBidField.clear();
                } else {
                    if (adminMessageLabel != null) {
                        adminMessageLabel.setText("Failed to delete item (might be locked)");
                        adminMessageLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
                    }
                }
            });
        }).start();
    }

    @FXML
    private void onBackHome() {
        MainApp.showHomePage();
    }

    @FXML
    private void onExportAuctions() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu Báo Cáo Đấu Giá");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("BaoCao_DauGia_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Title,Category,Status,Starting Bid,Current Bid,Seller,Winner,End Time");
                for (Auction a : rawAuctions) {
                    writer.printf("%d,\"%s\",\"%s\",%s,%.2f,%.2f,\"%s\",\"%s\",\"%s\"%n",
                            a.getId(),
                            a.getTitle() != null ? a.getTitle().replace("\"", "\"\"") : "",
                            a.getCategory() != null ? a.getCategory().replace("\"", "\"\"") : "",
                            a.getStatus(),
                            a.getStartingBid(),
                            a.getCurrentBid(),
                            a.getSeller() != null ? a.getSeller() : "",
                            a.getWinnerBidder() != null ? a.getWinnerBidder() : "",
                            a.getEndTime() != null ? a.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""
                    );
                }
                app.service.NotificationService.getInstance().showNotification(
                    "Success",
                    "Đã xuất báo cáo CSV thành công!",
                    app.service.NotificationService.NotificationType.SUCCESS
                );
            } catch (Exception e) {
                app.service.NotificationService.getInstance().showNotification(
                    "Error",
                    "Lỗi khi xuất file: " + e.getMessage(),
                    app.service.NotificationService.NotificationType.ERROR
                );
            }
        }
    }
}
