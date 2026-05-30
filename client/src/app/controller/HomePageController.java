package app.controller;

import app.MainApp;
import app.model.Auction;
import app.model.AuctionStatus;
import app.model.User;
import app.model.UserRole;
import app.service.IAuctionService;
import app.model.Item;
import java.util.stream.Collectors;
import java.time.Duration;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.animation.PauseTransition;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class HomePageController {
    private static final PseudoClass LIKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("liked");

    @FXML
    private BorderPane rootPane;

    @FXML
    private ScrollPane scrollPane;

    private int currentPage = 1;
    private final int pageSize = 20;
    private boolean isLoadingNextPage = false;
    private boolean hasMorePages = true;

    @FXML
    private VBox headerContainerVBox;

    @FXML
    private HBox headerTopHBox;

    @FXML
    private HBox searchWrapBox;

    @FXML
    private HBox navButtonsBox;

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane categoryBar;

    @FXML
    private FlowPane auctionGrid;


    @FXML
    private Label resultLabel;

    @FXML
    private Label sectionTitleLabel;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private Label watchlistBadgeLabel;

    @FXML
    private Button signInButton;

    @FXML
    private Button profileButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button sellerItemsButton;

    @FXML
    private Button adminDashboardButton;


    @FXML
    private HBox watchlistNavBox;

    @FXML
    private HBox notificationNavBox;

    @FXML
    private Label notificationBadgeLabel;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private VBox roleDashboardBox;

    @FXML
    private Label roleDashboardTitle;

    @FXML
    private Label roleDashboardSubtitle;

    @FXML
    private Button roleDashboardActionButton;

    @FXML
    private Label sellerDashboardItemsLabel;

    private List<Auction> auctions;
    private final java.util.Map<String, AuctionCardController> cardControllerMap = new java.util.HashMap<>();
    private String selectedCategory = "All";
    private String selectedSort = "Recommendation";
    private String selectedStatus = "All";
    private String selectedPriceRange = "All";
    private final IAuctionService auctionService = MainApp.getAuctionService();
    private app.service.AuctionObserver auctionObserver;

    @FXML
    private void onToggleCategories() {
        boolean nextVisible = !categoryBar.isVisible();
        categoryBar.setVisible(nextVisible);
        categoryBar.setManaged(nextVisible);
    }

    @FXML
    private void onOpenWatchlist() {
        MainApp.showWatchlistPage();
    }

    @FXML
    private void onShowFilterMenu(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        ContextMenu contextMenu = new ContextMenu();

        // --- SORT SECTION ---
        MenuItem sortHeader = new MenuItem("--- SORT BY ---");
        sortHeader.setDisable(true);
        contextMenu.getItems().add(sortHeader);

        String[] sortKeys = {"Recommendation", "PriceLow", "PriceHigh", "EndingSoon"};
        String[] sortLabels = {"Recommendation", "Price: Low to High", "Price: High to Low", "Ending Soon"};
        for (int i = 0; i < sortKeys.length; i++) {
            final String key = sortKeys[i];
            String label = sortLabels[i];
            if (key.equals(selectedSort)) {
                label = "✓ " + label;
            }
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> {
                selectedSort = key;
                resetPagination();
                renderAuctionCards();
            });
            contextMenu.getItems().add(item);
        }

        contextMenu.getItems().add(new SeparatorMenuItem());

        // --- STATUS FILTER SECTION ---
        MenuItem filterHeader = new MenuItem("--- STATUS FILTER ---");
        filterHeader.setDisable(true);
        contextMenu.getItems().add(filterHeader);

        String[] statusKeys = {"All", "Running", "Finished", "Canceled", "Paid"};
        String[] statusLabels = {"All", "Running (Đang diễn ra)", "Finished (Đã kết thúc)", "Canceled (Đã hủy)", "Paid (Đã thanh toán)"};
        for (int i = 0; i < statusKeys.length; i++) {
            final String key = statusKeys[i];
            String label = statusLabels[i];
            if (key.equals(selectedStatus)) {
                label = "✓ " + label;
            }
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> {
                selectedStatus = key;
                resetPagination();
                renderAuctionCards();
            });
            contextMenu.getItems().add(item);
        }

        contextMenu.getItems().add(new SeparatorMenuItem());

        // --- PRICE FILTER SECTION ---
        MenuItem priceHeader = new MenuItem("--- PRICE FILTER ---");
        priceHeader.setDisable(true);
        contextMenu.getItems().add(priceHeader);

        String[] priceKeys = {"All", "Under100", "100-500", "500-2000", "Over2000"};
        String[] priceLabels = {"Any Price", "Under $100", "$100 - $500", "$500 - $2,000", "Over $2,000"};
        for (int i = 0; i < priceKeys.length; i++) {
            final String key = priceKeys[i];
            String label = priceLabels[i];
            if (key.equals(selectedPriceRange)) {
                label = "✓ " + label;
            }
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> {
                selectedPriceRange = key;
                resetPagination();
                renderAuctionCards();
            });
            contextMenu.getItems().add(item);
        }

        contextMenu.show(source, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void onOpenSignIn() {
        MainApp.showSignInPage();
    }

    @FXML
    private void onOpenProfile() {
        MainApp.showProfilePage();
    }

    @FXML
    private void onLogout() {
        MainApp.getUserService().logout();
        MainApp.showHomePage();
    }

    @FXML
    private void onOpenSellerItems() {
        MainApp.showSellerItemsPage();
    }

    @FXML
    private void onOpenAdminDashboard() {
        MainApp.showAdminDashboardPage();
    }

    @FXML
    private void onRoleDashboardAction() {
        User currentUser = MainApp.getCurrentUser();
        if (currentUser == null) {
            MainApp.showSignInPage();
            return;
        }
        if (currentUser.getRole() == UserRole.SELLER) {
            MainApp.showSellerItemsPage();
            return;
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            MainApp.showAdminDashboardPage();
            return;
        }
        MainApp.showWatchlistPage();
    }


    private final Region responsiveSpacer = new Region();

    @FXML
    public void initialize() {
        MainApp.setupNotificationHover(notificationNavBox);
        responsiveSpacer.setId("responsiveSpacer");
        HBox.setHgrow(responsiveSpacer, Priority.ALWAYS);

        if (rootPane != null) {
            rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                double width = newVal.doubleValue();
                boolean isNarrow = width < 850; // Breakpoint for responsive header

                if (isNarrow && headerTopHBox.getChildren().contains(searchWrapBox)) {
                    // Move search block below branding and nav
                    headerTopHBox.getChildren().remove(searchWrapBox);
                    
                    if (!headerTopHBox.getChildren().contains(responsiveSpacer)) {
                        headerTopHBox.getChildren().add(1, responsiveSpacer);
                    }
                    
                    headerContainerVBox.getChildren().add(1, searchWrapBox);
                } else if (!isNarrow && headerContainerVBox.getChildren().contains(searchWrapBox)) {
                    // Move search block back inline
                    headerContainerVBox.getChildren().remove(searchWrapBox);
                    headerTopHBox.getChildren().remove(responsiveSpacer);
                    headerTopHBox.getChildren().add(1, searchWrapBox);
                }
            });
        }

        auctionGrid.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            if (width <= 0) return;
            
            // Standard gap for a clean, centered look
            auctionGrid.setHgap(24.0);
            auctionGrid.setVgap(32.0);
        });
        auctionGrid.setVgap(24.0);

        categoryBar.setVisible(false);
        categoryBar.setManaged(false);

        javafx.scene.shape.SVGPath emptyIcon = new javafx.scene.shape.SVGPath();
        emptyIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        emptyIcon.setFill(javafx.scene.paint.Color.valueOf("#94a3b8"));
        emptyIcon.setScaleX(3.0);
        emptyIcon.setScaleY(3.0);
        
        VBox emptyGraphic = new VBox(20);
        emptyGraphic.setAlignment(javafx.geometry.Pos.CENTER);
        emptyGraphic.setPadding(new Insets(40, 0, 40, 0));
        emptyGraphic.getChildren().addAll(emptyIcon, new Label());
        
        emptyStateLabel.setGraphic(emptyGraphic);
        emptyStateLabel.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
        emptyStateLabel.setAlignment(javafx.geometry.Pos.CENTER);
        emptyStateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -color-fg-subtle; -fx-padding: 40 0; -fx-font-weight: bold;");

        renderSkeletons();
        
        PauseTransition searchDebounce = new PauseTransition(javafx.util.Duration.millis(300));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            searchDebounce.playFromStart();
        });
        searchDebounce.setOnFinished(e -> {
            resetPagination();
            renderAuctionCards();
        });

        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                double vvalue = newVal.doubleValue();
                if (vvalue >= 0.9 && !isLoadingNextPage && hasMorePages) {
                    loadNextPage();
                }
            });
        }

        new Thread(() -> {
            List<app.model.Auction> fetchedAuctions = auctionService.getAuctions(1, pageSize);
            Platform.runLater(() -> {
                auctions = fetchedAuctions;
                renderCategories();
                updateWatchlistBadge();
                updateHeaderByAuth();
                renderAuctionCards();
                this.auctionObserver = () -> {
                    new Thread(() -> {
                        List<app.model.Auction> refreshed = auctionService.getAuctions(1, currentPage * pageSize);
                        Platform.runLater(() -> {
                            if (refreshed == null) return;
                            
                            boolean canGranularUpdate = auctions != null && refreshed.size() == auctions.size();
                            if (canGranularUpdate) {
                                for (int i = 0; i < refreshed.size(); i++) {
                                    if (!refreshed.get(i).getId().equals(auctions.get(i).getId())) {
                                        canGranularUpdate = false;
                                        break;
                                    }
                                }
                            }
                            
                            auctions = refreshed;
                            if (canGranularUpdate) {
                                for (Auction updated : refreshed) {
                                    AuctionCardController cardCtrl = cardControllerMap.get(updated.getId());
                                    if (cardCtrl != null) {
                                        cardCtrl.setAuction(updated);
                                    }
                                }
                            } else {
                                renderCategories();
                                renderAuctionCards();
                            }
                        });
                    }).start();
                };
                auctionService.registerObserver(this.auctionObserver);
                MainApp.setWatchlistRefreshCallback(this::updateWatchlistBadge);
                app.service.NetworkNotificationService.getInstance().getNotifications().addListener((javafx.collections.ListChangeListener<com.ltnc.auction.shared.dto.NotificationDto>) c -> {
                    javafx.application.Platform.runLater(this::updateNotificationBadge);
                });
                updateNotificationBadge();
            });
        }).start();
    }

    private void resetPagination() {
        currentPage = 1;
        hasMorePages = true;
        isLoadingNextPage = false;
    }

    private void loadNextPage() {
        isLoadingNextPage = true;
        int nextPage = currentPage + 1;
        new Thread(() -> {
            List<Auction> nextAuctions = auctionService.getAuctions(nextPage, pageSize);
            Platform.runLater(() -> {
                if (nextAuctions == null || nextAuctions.isEmpty()) {
                    hasMorePages = false;
                } else {
                    currentPage = nextPage;
                    auctions = auctionService.getCachedAuctions();
                    renderCategories();
                    renderAuctionCards();
                }
                isLoadingNextPage = false;
            });
        }).start();
    }

    private void renderSkeletons() {
        auctionGrid.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/skeleton-card.fxml"));
                auctionGrid.getChildren().add(loader.load());
            } catch (IOException ignored) {}
        }
    }

    private void renderCategories() {
        categoryBar.getChildren().clear();
        
        List<String> categories = new java.util.ArrayList<>();
        categories.add("All");
        
        if (auctions != null) {
            List<String> dynamicCats = auctions.stream()
                .map(Auction::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            categories.addAll(dynamicCats);
        }

        for (String category : categories) {
            Button button = new Button(category);
            button.getStyleClass().add("category-button");
            if (category.equals(selectedCategory)) {
                button.getStyleClass().add("category-button-active");
            }
            button.setOnAction(event -> {
                selectedCategory = category;
                resetPagination();
                renderCategories();
                renderAuctionCards();
            });
            categoryBar.getChildren().add(button);
        }
    }

    private void renderAuctionCards() {
        for (AuctionCardController c : cardControllerMap.values()) {
            c.cleanup();
        }
        cardControllerMap.clear();
        auctionGrid.getChildren().clear();

        auctions = auctionService.getCachedAuctions();

        User currentUser = MainApp.getCurrentUser();
        boolean isSeller = currentUser != null && currentUser.getRole() == UserRole.SELLER;

        List<Auction> filtered = auctions.stream()
                .filter(a -> !isSeller || a.getSeller().equals(currentUser.getEmail()))
                .filter(this::matchesCategory)
                .filter(this::matchesSearch)
                .filter(this::matchesStatus)
                .filter(this::matchesPriceRange)
                .sorted((a1, a2) -> {
                    int w1 = getStatusWeight(a1.getStatus());
                    int w2 = getStatusWeight(a2.getStatus());
                    if (w1 != w2) {
                        return Integer.compare(w2, w1);
                    }
                    if ("PriceLow".equals(selectedSort)) return Double.compare(a1.getCurrentBid(), a2.getCurrentBid());
                    if ("PriceHigh".equals(selectedSort)) return Double.compare(a2.getCurrentBid(), a1.getCurrentBid());
                    if ("EndingSoon".equals(selectedSort)) return a1.getEndTime().compareTo(a2.getEndTime());
                    
                    // Default: Recommendation
                    double score1 = calculateRecommendationScore(a1, currentUser);
                    double score2 = calculateRecommendationScore(a2, currentUser);
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());

        boolean defaultView = (searchField.getText() == null || searchField.getText().isBlank())
                && "All".equals(selectedCategory) && "All".equals(selectedStatus);

        if (isSeller) {
            sectionTitleLabel.setText(defaultView ? "My Auctions" : "My Auction Results");
            resultLabel.setText(filtered.size() + (filtered.size() == 1 ? " auction" : " auctions"));
            emptyStateLabel.setText("You have no active auctions yet. Go to Seller Items to create one.");
        } else {
            sectionTitleLabel.setText(defaultView ? "Featured Auctions" : "Search Results");
            resultLabel.setText(filtered.size() + (filtered.size() == 1 ? " auction found" : " auctions found"));
            emptyStateLabel.setText("No auctions found matching your criteria.");
        }

        emptyStateLabel.setVisible(filtered.isEmpty());
        emptyStateLabel.setManaged(filtered.isEmpty());

        // Optimized Batch Rendering
        if (filtered.size() > 12) {
            // Render first 12 immediately
            for (int i = 0; i < 12; i++) {
                auctionGrid.getChildren().add(createAuctionCard(filtered.get(i)));
            }
            // Render the rest in next pulse to avoid UI freeze
            Platform.runLater(() -> {
                for (int i = 12; i < filtered.size(); i++) {
                    auctionGrid.getChildren().add(createAuctionCard(filtered.get(i)));
                }
            });
        } else {
            for (Auction auction : filtered) {
                auctionGrid.getChildren().add(createAuctionCard(auction));
            }
        }
    }

    private Node createAuctionCard(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auction-card.fxml"));
            Node card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.setAuction(auction);
            cardControllerMap.put(auction.getId(), controller);
            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading card");
        }
    }

    private boolean matchesCategory(Auction auction) {
        return "All".equals(selectedCategory)
                || selectedCategory.equalsIgnoreCase(auction.getCategory());
    }

    private boolean matchesSearch(Auction auction) {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalized = query.toLowerCase();
        return auction.getTitle().toLowerCase().contains(normalized)
                || auction.getDescription().toLowerCase().contains(normalized);
    }

    private boolean matchesStatus(Auction auction) {
        if ("All".equalsIgnoreCase(selectedStatus)) {
            return true;
        }
        AuctionStatus status = auction.getStatus();
        if (status == null) {
            return false;
        }
        return switch (selectedStatus) {
            case "Running" -> status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN;
            case "Finished" -> status == AuctionStatus.FINISHED;
            case "Canceled" -> status == AuctionStatus.CANCELED;
            case "Paid" -> status == AuctionStatus.PAID;
            default -> true;
        };
    }

    private boolean matchesPriceRange(Auction auction) {
        if ("All".equals(selectedPriceRange)) {
            return true;
        }
        double price = auction.getCurrentBid();
        return switch (selectedPriceRange) {
            case "Under100" -> price < 100.0;
            case "100-500" -> price >= 100.0 && price <= 500.0;
            case "500-2000" -> price >= 500.0 && price <= 2000.0;
            case "Over2000" -> price > 2000.0;
            default -> true;
        };
    }

    private int getStatusWeight(AuctionStatus status) {
        if (status == AuctionStatus.RUNNING) return 2;
        if (status == AuctionStatus.OPEN) return 1;
        return 0; // FINISHED, PAID, CANCELED
    }

    private double calculateRecommendationScore(Auction auction, User currentUser) {
        double score = 0.0;
        
        // 1. Base Score: Is it active?
        boolean active = (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) 
                         && auction.getEndTime().isAfter(java.time.LocalDateTime.now());
        if (!active) return -10000.0; // Push inactive to the bottom
        
        // 2. Proximity to End Time (Urgency - 0 to 50 points)
        long minutesLeft = java.time.Duration.between(java.time.LocalDateTime.now(), auction.getEndTime()).toMinutes();
        if (minutesLeft < 60) score += 50.0; // Ending in 1 hr!
        else if (minutesLeft < 1440) score += 30.0; // Ending today
        else score += 10.0;
        
        // 3. Global Popularity (Collaborative Filtering proxy - 0 to 100 points)
        int totalBids = auction.getBids().size();
        score += Math.min(100.0, totalBids * 5.0); // 5 points per bid, max 100
        
        // 4. Personalized Content-Based Filtering (User Data - 0 to 200 points)
        if (currentUser != null && currentUser.getRole() == UserRole.BIDDER) {
            long categoryBidCount = 0;
            long myBidsOnThisAuction = 0;
            
            // Analyze historical bids
            for (Auction a : auctions) {
                boolean iBidOnThis = a.getBids().stream().anyMatch(b -> 
                        b.getBidder().equals(currentUser.getEmail()) || b.getBidder().equals(currentUser.getFullName()));
                if (iBidOnThis) {
                    if (a.getCategory().equals(auction.getCategory())) {
                        categoryBidCount++;
                    }
                    if (a.getId().equals(auction.getId())) {
                        myBidsOnThisAuction++;
                    }
                }
            }
            
            // Boost matching categories
            score += Math.min(100.0, categoryBidCount * 20.0);
            
            // 5. Watchlist Boost
            if (MainApp.isLiked(auction.getId())) {
                score += 80.0;
            }
            
            // 6. Active Participation Boost
            if (myBidsOnThisAuction > 0) {
                score += 50.0;
            }
        }
        
        return score;
    }

    private void openAuctionDetailForBid(Auction auction) {
        if (!MainApp.isLoggedIn()) {
            MainApp.setPendingAuction(auction.getId());
            MainApp.showSignInPage();
            return;
        }
        MainApp.showAuctionDetail(auction);
    }

    public void updateWatchlistBadge() {
        int count = MainApp.getWatchlistCount();
        watchlistBadgeLabel.setText(String.valueOf(count));
        watchlistBadgeLabel.setVisible(count > 0);
        watchlistBadgeLabel.setManaged(count > 0);
    }

    public void updateNotificationBadge() {
        if (notificationBadgeLabel == null) return;
        long count = app.service.NetworkNotificationService.getInstance().getUnreadCount();
        notificationBadgeLabel.setText(String.valueOf(count));
        notificationBadgeLabel.setVisible(count > 0);
        notificationBadgeLabel.setManaged(count > 0);
    }

    private void updateHeaderByAuth() {
        User currentUser = MainApp.getCurrentUser();
        boolean isLoggedIn = currentUser != null;
        boolean isAdmin = isLoggedIn && currentUser.getRole() == UserRole.ADMIN;
        boolean isSeller = isLoggedIn && currentUser.getRole() == UserRole.SELLER;

        signInButton.setVisible(!isLoggedIn);
        signInButton.setManaged(!isLoggedIn);

        logoutButton.setVisible(isLoggedIn);
        logoutButton.setManaged(isLoggedIn);
        profileButton.setVisible(isLoggedIn);
        profileButton.setManaged(isLoggedIn);

        if (isLoggedIn) {
            sellerItemsButton.setVisible(isSeller);
            sellerItemsButton.setManaged(isSeller);
            adminDashboardButton.setVisible(isAdmin);
            adminDashboardButton.setManaged(isAdmin);
        } else {
            sellerItemsButton.setVisible(false);
            sellerItemsButton.setManaged(false);
            adminDashboardButton.setVisible(false);
            adminDashboardButton.setManaged(false);
        }

        boolean isBidder = !isLoggedIn || (isLoggedIn && currentUser.getRole() == UserRole.BIDDER);

        // Watchlist only for Bidders
        watchlistNavBox.setVisible(isBidder);
        watchlistNavBox.setManaged(isBidder);

        // Notifications for all logged-in users
        if (notificationNavBox != null) {
            notificationNavBox.setVisible(isLoggedIn);
            notificationNavBox.setManaged(isLoggedIn);
            if (isLoggedIn) {
                app.service.NetworkNotificationService.getInstance().fetchNotifications();
            }
        }

        // Update titles based on role
        if (isAdmin) {
            pageTitleLabel.setText("Platform Overview");
            pageSubtitleLabel.setText("Monitor all live auctions and manage platform activity.");
        } else if (isSeller) {
            pageTitleLabel.setText("Seller Dashboard");
            pageSubtitleLabel.setText("Manage your listings and monitor your sales performance.");
        } else {
            pageTitleLabel.setText("Discover Treasures");
            pageSubtitleLabel.setText("Find the best items from around the world.");
        }

        updateRoleDashboard(currentUser);
    }

    private void updateRoleDashboard(User currentUser) {
        // Banner is no longer needed on the home page as requested
        roleDashboardBox.setVisible(false);
        roleDashboardBox.setManaged(false);
    }

    @FXML
    private void onToggleLang() {
        MainApp.toggleLang();
    }

    @FXML
    private void onToggleTheme() {
        MainApp.toggleDarkMode();
    }

    public void cleanup() {
        if (auctionObserver != null) {
            auctionService.unregisterObserver(auctionObserver);
        }
        for (AuctionCardController c : cardControllerMap.values()) {
            c.cleanup();
        }
        cardControllerMap.clear();
    }
}
