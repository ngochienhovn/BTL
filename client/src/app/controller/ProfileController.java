package app.controller;

import app.MainApp;
import app.ThemeManager;
import app.model.User;
import app.service.AuctionObserver;
import app.service.NetworkAuctionService;
import app.service.NotificationService;
import com.ltnc.auction.shared.dto.WalletDto;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import app.model.Auction;
import app.model.AuctionStatus;
import app.model.Bid;
import app.service.BidLogEntry;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class ProfileController {

    @FXML
    private VBox statsCard;

    @FXML
    private Label totalBidsLabel;

    @FXML
    private Label winningCountLabel;

    @FXML
    private Label wonCountLabel;

    @FXML
    private Label lostCountLabel;

    @FXML
    private Label watchlistCountLabel;

    @FXML
    private Label walletBalanceLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label emailLabel2;

    @FXML
    private Label roleLabel;

    @FXML
    private Label accountTypeLabel;

    @FXML
    private Label memberSinceLabel;

    private User boundUser;
    private boolean observerRegistered;
    private final NetworkAuctionService netAuction = NetworkAuctionService.getInstance();

    private final AuctionObserver statsObserver = () -> Platform.runLater(this::refreshStats);

    public void bindUser(User user) {
        this.boundUser = user;
        if (user == null) {
            nameLabel.setText(ThemeManager.get("profile.guest"));
            emailLabel.setText("-");
            roleLabel.setText("-");
            if (accountTypeLabel != null) {
                accountTypeLabel.setText("-");
            }
            if (memberSinceLabel != null) {
                memberSinceLabel.setText("-");
            }
            if (statsCard != null) {
                statsCard.setVisible(false);
                statsCard.setManaged(false);
            }
            if (observerRegistered) {
                netAuction.unregisterObserver(statsObserver);
                observerRegistered = false;
            }
            return;
        }
        nameLabel.setText(user.getFullName());
        emailLabel.setText(user.getEmail());
        if (emailLabel2 != null) {
            emailLabel2.setText(user.getEmail());
        }
        String roleDisplay = switch (user.getRole()) {
            case BIDDER -> ThemeManager.get("user.role.bidder");
            case SELLER -> ThemeManager.get("user.role.seller");
            case ADMIN -> ThemeManager.get("user.role.admin");
        };
        roleLabel.setText(roleDisplay);
        if (accountTypeLabel != null) {
            accountTypeLabel.setText(roleDisplay);
        }
        if (memberSinceLabel != null) {
            memberSinceLabel.setText("2026");
        }

        boolean isBidder = user.getRole() == app.model.UserRole.BIDDER;
        if (statsCard != null) {
            statsCard.setVisible(isBidder);
            statsCard.setManaged(isBidder);
        }

        if (!observerRegistered) {
            netAuction.registerObserver(statsObserver);
            observerRegistered = true;
        }
        refreshStats();
    }

    private void refreshStats() {
        if (boundUser == null || boundUser.getRole() != app.model.UserRole.BIDDER) {
            return;
        }

        new Thread(() -> {
            // Refresh Wallet Info
            netAuction.fetchWallet(boundUser);
            WalletDto wallet = netAuction.getCachedWallet();
            
            String userEmail = boundUser.getEmail();

            List<BidLogEntry> logs = netAuction.getBidLogs();
            long totalBids = logs.stream()
                    .filter(b -> userEmail.equalsIgnoreCase(b.bidder()))
                    .count();

            List<Auction> allAuctions = netAuction.getAuctions();
            List<String> activeReserves = getActiveReserves(allAuctions);
            
            long wonCount = allAuctions.stream()
                    .filter(a -> (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID)
                            && userEmail.equalsIgnoreCase(a.getWinnerBidder()))
                    .count();

            long lostCount = allAuctions.stream()
                    .filter(a -> (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID)
                            && !userEmail.equalsIgnoreCase(a.getWinnerBidder()))
                    .filter(a -> a.getBids().stream().anyMatch(b -> userEmail.equalsIgnoreCase(b.getBidderEmail())))
                    .count();

            int watchCount = MainApp.getWatchlistCount();

            Platform.runLater(() -> {
                if (wallet != null && walletBalanceLabel != null) {
                    walletBalanceLabel.setText(formatCurrency(wallet.available != null ? wallet.available : 0));
                }
                totalBidsLabel.setText(String.valueOf(totalBids));
                winningCountLabel.setText(String.valueOf(activeReserves.size()));
                wonCountLabel.setText(String.valueOf(wonCount));
                lostCountLabel.setText(String.valueOf(lostCount));
                watchlistCountLabel.setText(String.valueOf(watchCount));
            });
        }).start();
    }

    private List<String> getActiveReserves(List<Auction> allAuctions) {
        List<String> list = new ArrayList<>();
        if (boundUser == null) {
            return list;
        }
        String userEmail = boundUser.getEmail();
        for (Auction a : allAuctions) {
            if (a.getStatus() == AuctionStatus.RUNNING) {
                Bid highest = a.getBids().stream()
                        .max(Comparator.comparingDouble(Bid::getAmount))
                        .orElse(null);
                if (highest != null && userEmail.equalsIgnoreCase(highest.getBidderEmail())) {
                    list.add(a.getTitle() + ": " + formatCurrency(highest.getAmount()));
                }
            }
        }
        return list;
    }

    @FXML
    private void onEditName() {
        if (boundUser == null) return;
        TextInputDialog dialog = new TextInputDialog(boundUser.getFullName());
        dialog.setTitle(ThemeManager.get("profile.editName.title"));
        dialog.setHeaderText(ThemeManager.get("profile.editName.header"));
        dialog.setContentText(ThemeManager.get("profile.editName.prompt"));
        dialog.showAndWait().ifPresent(newName -> {
            String finalNewName = newName.trim();
            if (finalNewName.isEmpty()) {
                NotificationService.getInstance().showNotification(
                    ThemeManager.get("notification.error"),
                    ThemeManager.get("error.email.empty"),
                    NotificationService.NotificationType.ERROR
                );
                return;
            }
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = MainApp.getUserService().updateUser(boundUser.getEmail(), finalNewName, null, boundUser.getRole());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        NotificationService.getInstance().showNotification(
                            ThemeManager.get("notification.success"),
                            ThemeManager.get("profile.editName.success"),
                            NotificationService.NotificationType.SUCCESS
                        );
                        bindUser(MainApp.getCurrentUser());
                    } else {
                        NotificationService.getInstance().showNotification(
                            ThemeManager.get("notification.error"),
                            ThemeManager.get("bid.error.generic"),
                            NotificationService.NotificationType.ERROR
                        );
                    }
                });
            }).start();
        });
    }

    @FXML
    private void onChangePassword() {
        if (boundUser == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(ThemeManager.get("profile.changePassword.title"));
        dialog.setHeaderText(ThemeManager.get("profile.changePassword.header"));
        dialog.setContentText(ThemeManager.get("profile.changePassword.prompt"));
        dialog.showAndWait().ifPresent(newPassword -> {
            String finalNewPassword = newPassword.trim();
            if (!validatePassword(finalNewPassword)) {
                return;
            }
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean ok = MainApp.getUserService().updateUser(boundUser.getEmail(), boundUser.getFullName(), finalNewPassword, boundUser.getRole());
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (ok) {
                        NotificationService.getInstance().showNotification(
                            ThemeManager.get("notification.success"),
                            ThemeManager.get("profile.changePassword.success"),
                            NotificationService.NotificationType.SUCCESS
                        );
                    } else {
                        NotificationService.getInstance().showNotification(
                            ThemeManager.get("notification.error"),
                            ThemeManager.get("bid.error.generic"),
                            NotificationService.NotificationType.ERROR
                        );
                    }
                });
            }).start();
        });
    }

    private boolean validatePassword(String password) {
        if (password == null || password.isBlank()) {
            NotificationService.getInstance().showNotification(
                ThemeManager.get("notification.error"),
                ThemeManager.get("error.password.empty"),
                NotificationService.NotificationType.ERROR
            );
            return false;
        }
        if (password.length() < 8) {
            NotificationService.getInstance().showNotification(
                ThemeManager.get("notification.error"),
                ThemeManager.get("error.password.short"),
                NotificationService.NotificationType.ERROR
            );
            return false;
        }
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        if (!hasSpecial) {
            NotificationService.getInstance().showNotification(
                ThemeManager.get("notification.error"),
                ThemeManager.get("error.password.special"),
                NotificationService.NotificationType.ERROR
            );
            return false;
        }
        return true;
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    @FXML
    private void onDeposit() {
        if (boundUser == null) return;
        
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Deposit Funds");
        dialog.setHeaderText("Add funds to your account");
        dialog.setContentText("Enter amount to deposit ($):");
        
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr.trim());
                if (amount <= 0) {
                    NotificationService.getInstance().showNotification("Error", "Amount must be positive", NotificationService.NotificationType.ERROR);
                    return;
                }
                
                app.service.LoadingService.getInstance().show();
                new Thread(() -> {
                    boolean success = netAuction.deposit(boundUser, amount);
                    Platform.runLater(() -> {
                        app.service.LoadingService.getInstance().hide();
                        if (success) {
                            NotificationService.getInstance().showNotification("Success", "Deposited " + formatCurrency(amount) + " successfully!", NotificationService.NotificationType.SUCCESS);
                            refreshStats();
                        } else {
                            NotificationService.getInstance().showNotification("Error", "Deposit failed. Please try again.", NotificationService.NotificationType.ERROR);
                        }
                    });
                }).start();
            } catch (NumberFormatException e) {
                NotificationService.getInstance().showNotification("Error", "Invalid amount format", NotificationService.NotificationType.ERROR);
            }
        });
    }

    @FXML
    private void onWithdraw() {
        if (boundUser == null) return;
        
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Withdraw Funds");
        dialog.setHeaderText("Withdraw funds from your account");
        dialog.setContentText("Enter amount to withdraw ($):");
        
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr.trim());
                if (amount <= 0) {
                    NotificationService.getInstance().showNotification("Error", "Amount must be positive", NotificationService.NotificationType.ERROR);
                    return;
                }
                
                app.service.LoadingService.getInstance().show();
                new Thread(() -> {
                    boolean success = netAuction.withdraw(boundUser, amount);
                    Platform.runLater(() -> {
                        app.service.LoadingService.getInstance().hide();
                        if (success) {
                            NotificationService.getInstance().showNotification("Success", "Withdrew " + formatCurrency(amount) + " successfully!", NotificationService.NotificationType.SUCCESS);
                            refreshStats();
                        } else {
                            NotificationService.getInstance().showNotification("Error", "Withdrawal failed. Check your balance or try again.", NotificationService.NotificationType.ERROR);
                        }
                    });
                }).start();
            } catch (NumberFormatException e) {
                NotificationService.getInstance().showNotification("Error", "Invalid amount format", NotificationService.NotificationType.ERROR);
            }
        });
    }

    @FXML
    private void onBackHome() {
        cleanup();
        MainApp.showHomePage();
    }

    public void cleanup() {
        if (observerRegistered) {
            netAuction.unregisterObserver(statsObserver);
            observerRegistered = false;
        }
    }
}
