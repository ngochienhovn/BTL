package app;

import app.config.ClientConfig;
import app.controller.AuctionDetailController;
import app.controller.AdminDashboardController;
import app.controller.ProfileController;
import app.controller.SellerItemsController;
import app.controller.WatchlistController;
import app.controller.HomePageController;
import app.model.Auction;
import app.model.User;
import app.model.UserRole;
import app.net.SocketClient;
import app.service.IAuctionService;
import app.service.IUserService;
import app.service.IItemService;
import app.service.NetworkAuctionService;
import app.service.NetworkUserService;
import app.service.NetworkItemService;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * ============================================================
 *  ĐIỂM KHỞI ĐỘNG CỦA CLIENT (JavaFX Entry Point)
 *  Phụ trách: Thành viên Frontend (TV2) + Network (TV3)
 * ============================================================
 *
 *  MainApp là trung tâm điều phối của toàn bộ ứng dụng client:
 *
 *  1. KHỞI ĐỘNG: Áp dụng theme → kết nối server → hiển thị màn hình đầu tiên.
 *     Ứng dụng BẮT BUỘC phải kết nối được với Server qua Socket. Nếu thất bại sẽ thoát.
 *
 *  2. ĐIỀU HƯỚNG MÀN HÌNH (Navigation):
 *     Tất cả các hàm show*() đều load FXML → tạo scene mới → hiển thị.
 *     Controller của màn hình được JavaFX tạo tự động từ annotation fx:controller trong FXML.
 *
 *  3. TRẠNG THÁI TOÀN CỤC (Global State):
 *     - currentUserEmail: email người đang đăng nhập.
 *     - likedAuctionIds: watchlist (lưu trong bộ nhớ phiên làm việc).
 *     - auctionService / userService / itemService: service giao tiếp với Server.
 *
 *  Cấu trúc MVC phía Client:
 *    View (FXML)  ←→  Controller (Java)  ←→  Service (Network)  ←→  SocketClient
 */
public class MainApp extends Application {

    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    /** Màn hình hiện tại — dùng để reload i18n mà không nhảy về home. */
    public enum UiRoute {
        HOME,
        AUCTION_DETAIL,
        WATCHLIST,
        SIGN_IN,
        SIGN_UP,
        PROFILE,
        SELLER_ITEMS,
        ADMIN
    }

    private static Stage primaryStage;

    /** Watchlist: tập hợp ID các phiên người dùng đang theo dõi. */
    private static final Set<String> likedAuctionIds = new LinkedHashSet<>();

    private static String currentUserEmail;
    private static String pendingAuctionId; // phiên muốn xem nhưng chưa login

    private static UiRoute currentRoute = UiRoute.HOME;
    private static String lastDetailAuctionId;
    
    /** Callback to refresh UI elements (like badges) when watchlist changes. */
    private static Runnable watchlistRefreshCallback;

    // ── Services – giao tiếp giữa Controller và dữ liệu ─────────────────
    private static IAuctionService auctionService;
    private static IUserService userService;
    private static IItemService itemService;

    /**
     * Điểm vào của JavaFX Application.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        MainApp.primaryStage = primaryStage;
        MainApp.primaryStage.setTitle("BidMaster");
        MainApp.primaryStage.setMinWidth(640.0);
        MainApp.primaryStage.setMinHeight(480.0);

        // AtlantaFX theme phải được áp dụng trước khi tạo bất kỳ Scene nào
        ThemeManager.applyInitialTheme();

        // Initialize notification service
        app.service.NotificationService.getInstance().setMainStage(primaryStage);
        // Initialize loading service
        app.service.LoadingService.getInstance().setMainStage(primaryStage);

        // Bắt buộc kết nối với server
        boolean connected = connectToServer();
        if (connected) {
            auctionService = NetworkAuctionService.getInstance();
            userService = NetworkUserService.getInstance();
            itemService = NetworkItemService.getInstance();
            auctionService.initialize(List.of());
            SocketClient.getInstance().setOnReconnected(() -> {
                NetworkUserService.getInstance().silentReLogin();
                NetworkAuctionService.getInstance().getAuctions();
            });
            LOG.info("Connected to BidMaster server at " + ClientConfig.getServerHost() + ":" + ClientConfig.getServerPort());
        } else {
            LOG.severe("Failed to connect to server at " + ClientConfig.getServerHost() + ":" + ClientConfig.getServerPort());
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(ThemeManager.get("app.connect.error.title"));
            alert.setHeaderText(ThemeManager.get("app.connect.error.header"));
            alert.setContentText(MessageFormat.format(
                    ThemeManager.get("app.connect.error.detail"),
                    ClientConfig.getServerHost(),
                    String.valueOf(ClientConfig.getServerPort())));
            alert.showAndWait();
            Platform.exit();
            return;
        }

        showHomePage();
        MainApp.primaryStage.show();
    }

    /** Thử kết nối TCP tới server. Trả về true nếu thành công. */
    private static boolean connectToServer() {
        return SocketClient.getInstance().connect();
    }

    /** Chuyển đổi Dark/Light Mode cho toàn bộ ứng dụng. */
    public static void toggleDarkMode() {
        ThemeManager.toggleTheme();
    }

    /** Chuyển đổi ngôn ngữ EN/VI và reload trang hiện tại. */
    public static void toggleLang() {
        ThemeManager.toggleLang();
        reloadCurrentRoute();
    }

    /** Reload FXML của màn đang mở (sau đổi ngôn ngữ / theme text). */
    public static void reloadCurrentRoute() {
        try {
            switch (currentRoute) {
                case HOME -> showHomePage();
                case AUCTION_DETAIL -> {
                    if (lastDetailAuctionId == null || lastDetailAuctionId.isBlank()) {
                        showHomePage();
                        return;
                    }
                    Auction fresh = getAuctions().stream()
                            .filter(a -> lastDetailAuctionId.equals(a.getId()))
                            .findFirst()
                            .orElse(null);
                    if (fresh != null) {
                        showAuctionDetail(fresh);
                    } else {
                        showHomePage();
                    }
                }
                case WATCHLIST -> showWatchlistPage();
                case SIGN_IN -> showSignInPage();
                case SIGN_UP -> showSignUpPage();
                case PROFILE -> showProfilePage();
                case SELLER_ITEMS -> showSellerItemsPage();
                case ADMIN -> showAdminDashboardPage();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "reloadCurrentRoute failed", e);
            showHomePage();
        }
    }

    private static Object currentController;

    /**
     * Thay root node của Scene hiện tại (không tạo Stage mới).
     * Cách này giữ lại kích thước cửa sổ, mượt hơn so với tạo Stage mới.
     */
    private static void switchScene(Parent root, Object controller, double defaultWidth, double defaultHeight) {
        // Perform cleanup on old controller if needed
        if (currentController != null) {
            try {
                if (currentController instanceof AuctionDetailController) {
                    ((AuctionDetailController) currentController).cleanup();
                } else if (currentController instanceof AdminDashboardController) {
                    ((AdminDashboardController) currentController).cleanup();
                } else if (currentController instanceof SellerItemsController) {
                    ((SellerItemsController) currentController).cleanup();
                } else if (currentController instanceof HomePageController) {
                    ((HomePageController) currentController).cleanup();
                } else if (currentController instanceof ProfileController) {
                    ((ProfileController) currentController).cleanup();
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cleanup failed for controller: " + currentController.getClass().getName(), e);
            }
        }
        currentController = controller;

        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(root, defaultWidth, defaultHeight));
        } else {
            Parent oldRoot = primaryStage.getScene().getRoot();
            
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), oldRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                primaryStage.getScene().setRoot(root);
                root.setOpacity(0);
                root.setTranslateY(20);
                
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                
                javafx.animation.TranslateTransition slideUp = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), root);
                slideUp.setFromY(20);
                slideUp.setToY(0);
                slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                
                javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(fadeIn, slideUp);
                parallel.play();
            });
            fadeOut.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ĐIỀU HƯỚNG MÀN HÌNH (Navigation)
    // ══════════════════════════════════════════════════════════════════

    public static void setupNotificationHover(javafx.scene.layout.HBox navBox) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/notification-list.fxml"), bundle);
            Parent root = loader.load();
            app.controller.NotificationListController controller = loader.getController();
            controller.setPopup(popup);
            
            popup.getContent().add(root);

            navBox.setOnMouseEntered(e -> {
                if (!popup.isShowing()) {
                    javafx.geometry.Bounds bounds = navBox.localToScreen(navBox.getBoundsInLocal());
                    popup.show(navBox, bounds.getMaxX() - 400, bounds.getMaxY() + 10);
                }
            });

            navBox.setOnMouseExited(e -> {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pause.setOnFinished(ev -> {
                    if (!root.isHover()) {
                        popup.hide();
                    }
                });
                pause.play();
            });

            root.setOnMouseExited(e -> {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pause.setOnFinished(ev -> {
                    if (!navBox.isHover()) {
                        popup.hide();
                    }
                });
                pause.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showHomePage() {
        currentRoute = UiRoute.HOME;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/home-page.fxml"), bundle);
            Parent root = loader.load();
            switchScene(root, loader.getController(), 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load home page", exception);
        }
    }

    public static void showAuctionDetail(Auction auction) {
        currentRoute = UiRoute.AUCTION_DETAIL;
        lastDetailAuctionId = auction != null ? auction.getId() : null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/auction-detail.fxml"), bundle);
            Parent root = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuction(auction);
            switchScene(root, controller, 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load auction detail", exception);
        }
    }

    public static void showWatchlistPage() {
        currentRoute = UiRoute.WATCHLIST;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/watchlist.fxml"), bundle);
            Parent root = loader.load();
            WatchlistController controller = loader.getController();
            controller.loadWatchlist(getLikedAuctions());
            switchScene(root, controller, 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load watchlist page", exception);
        }
    }

    public static void showSignInPage() {
        currentRoute = UiRoute.SIGN_IN;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/sign-in.fxml"), bundle);
            Parent root = loader.load();
            switchScene(root, loader.getController(), 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load sign in page", exception);
        }
    }

    public static void showSignUpPage() {
        currentRoute = UiRoute.SIGN_UP;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/sign-up.fxml"), bundle);
            Parent root = loader.load();
            switchScene(root, loader.getController(), 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load sign up page", exception);
        }
    }

    public static void showProfilePage() {
        currentRoute = UiRoute.PROFILE;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/profile.fxml"), bundle);
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.bindUser(getCurrentUser());
            switchScene(root, controller, 900, 640);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load profile page", exception);
        }
    }

    public static void showSellerItemsPage() {
        currentRoute = UiRoute.SELLER_ITEMS;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/seller-items.fxml"), bundle);
            Parent root = loader.load();
            SellerItemsController controller = loader.getController();
            controller.loadSellerItems(getCurrentUser());
            switchScene(root, controller, 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load seller items page", exception);
        }
    }

    public static void showAdminDashboardPage() {
        currentRoute = UiRoute.ADMIN;
        lastDetailAuctionId = null;
        try {
            ResourceBundle bundle = ThemeManager.getBundle();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/admin-dashboard.fxml"), bundle);
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.loadAuctions(auctionService.getAdminViewAuctions());
            switchScene(root, controller, 1100, 720);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load admin dashboard page", exception);
        }
    }

    public static List<Auction> getAuctions() {
        return auctionService.getAuctions();
    }

    public static boolean isLoggedIn() {
        return userService.isLoggedIn();
    }

    public static void completeLogin(String email) {
        currentUserEmail = email;
    }

    public static void completeLogin(User user) {
        currentUserEmail = user != null ? user.getEmail() : null;
    }

    public static void setPendingAuction(String auctionId) {
        pendingAuctionId = auctionId;
    }

    public static void redirectAfterLoginIfNeeded() {
        if (pendingAuctionId == null || pendingAuctionId.isBlank()) {
            showHomePage();
            return;
        }

        List<Auction> allAuctions = auctionService.getAuctions();
        Auction pending = allAuctions.stream()
                .filter(auction -> pendingAuctionId.equals(auction.getId()))
                .findFirst()
                .orElse(null);
        pendingAuctionId = null;

        if (pending != null) {
            showAuctionDetail(pending);
        } else {
            showHomePage();
        }
    }

    public static User getCurrentUser() {
        return userService.getCurrentUser();
    }

    public static UserRole getCurrentUserRole() {
        User current = getCurrentUser();
        return current != null ? current.getRole() : null;
    }

    public static IUserService getUserService() {
        return userService;
    }

    public static IAuctionService getAuctionService() {
        return auctionService;
    }

    public static IItemService getItemService() {
        return itemService;
    }

    public static boolean isLiked(String auctionId) {
        return likedAuctionIds.contains(auctionId);
    }

    public static void toggleLiked(String auctionId) {
        if (likedAuctionIds.contains(auctionId)) {
            likedAuctionIds.remove(auctionId);
        } else {
            likedAuctionIds.add(auctionId);
        }
        
        // Notify UI to refresh badges/counts
        if (watchlistRefreshCallback != null) {
            watchlistRefreshCallback.run();
        }
    }

    public static void setWatchlistRefreshCallback(Runnable callback) {
        MainApp.watchlistRefreshCallback = callback;
    }

    public static int getWatchlistCount() {
        return likedAuctionIds.size();
    }

    public static List<Auction> getLikedAuctions() {
        List<Auction> allAuctions = auctionService.getAuctions();
        return allAuctions.stream()
                .filter(auction -> likedAuctionIds.contains(auction.getId()))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
