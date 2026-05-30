package app.controller;

import app.service.NetworkNotificationService;
import com.ltnc.auction.shared.dto.NotificationDto;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class NotificationListController {
    @FXML
    private ListView<NotificationDto> notificationListView;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Label headerLabel;
    @FXML
    private Label markAllReadLabel;
    @FXML
    private Label emptyLabel;

    private Stage dialogStage;

    @FXML
    public void initialize() {
        if (headerLabel != null) headerLabel.setText(app.ThemeManager.get("notification.header"));
        if (markAllReadLabel != null) markAllReadLabel.setText(app.ThemeManager.get("notification.markAllRead"));
        if (emptyLabel != null) emptyLabel.setText(app.ThemeManager.get("notification.empty"));

        notificationListView.setItems(NetworkNotificationService.getInstance().getNotifications());
        
        notificationListView.setCellFactory(lv -> new ListCell<NotificationDto>() {
            @Override
            protected void updateItem(NotificationDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // Set cell padding & background to transparent to avoid default border/margins interference
                    setStyle("-fx-background-color: transparent; -fx-padding: 6 12 6 12;");
                    
                    HBox container = new HBox(16);
                    container.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                    
                    String baseStyle = "-fx-padding: 16; -fx-background-radius: 12; -fx-transition: all 0.2s;";
                    String unreadStyle = baseStyle + " -fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-radius: 12; -fx-border-width: 1;";
                    String readStyle = baseStyle + " -fx-background-color: transparent; -fx-border-color: transparent; -fx-border-radius: 12; -fx-border-width: 1;";
                    String hoverStyle = baseStyle + " -fx-background-color: -color-bg-inset; -fx-border-color: -color-border-muted; -fx-border-radius: 12; -fx-border-width: 1; -fx-cursor: hand;";
                    
                    container.setStyle(!item.isRead ? unreadStyle : readStyle);
                    
                    container.setOnMouseEntered(e -> container.setStyle(hoverStyle));
                    container.setOnMouseExited(e -> container.setStyle(!item.isRead ? unreadStyle : readStyle));
                    
                    // Professional Icon Container
                    HBox iconContainer = new HBox();
                    iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
                    iconContainer.setMinSize(42, 42);
                    iconContainer.setMaxSize(42, 42);
                    
                    Label icon = new Label();
                    icon.setStyle("-fx-font-size: 20px;");
                    if ("SUCCESS".equals(item.type)) {
                        icon.setText("🎉");
                        iconContainer.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 21;"); // light green
                    } else if ("WARNING".equals(item.type)) {
                        icon.setText("⚠️");
                        iconContainer.setStyle("-fx-background-color: #fef08a; -fx-background-radius: 21;"); // light yellow
                    } else {
                        icon.setText("🔔");
                        iconContainer.setStyle("-fx-background-color: #e0f2fe; -fx-background-radius: 21;"); // light blue
                    }
                    iconContainer.getChildren().add(icon);
                    
                    VBox texts = new VBox(6);
                    Label title = new Label(item.title);
                    title.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: -color-fg-default;");
                    
                    Label message = new Label(item.message);
                    message.setWrapText(true);
                    message.prefWidthProperty().bind(notificationListView.widthProperty().subtract(145)); // Perfect margin subtraction
                    message.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px; -fx-line-spacing: 0.2em;");
                    
                    String formattedDate = item.createdAt.replace("T", " ");
                    if (formattedDate.contains(".")) {
                        formattedDate = formattedDate.substring(0, formattedDate.indexOf("."));
                    }
                    
                    // Footer container holding date and mark read/unread link
                    HBox footer = new HBox(4);
                    footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    Label date = new Label(formattedDate);
                    date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");
                    
                    Label separator = new Label("•");
                    separator.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
                    
                    Label actionLink = new Label(item.isRead ? app.ThemeManager.get("notification.markUnread") : app.ThemeManager.get("notification.markRead"));
                    String linkStyleNormal = "-fx-text-fill: -color-accent-emphasis; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-background-color: transparent;";
                    String linkStyleHover = "-fx-text-fill: -color-accent-emphasis; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-background-color: -color-accent-subtle; -fx-underline: true;";
                    
                    actionLink.setStyle(linkStyleNormal);
                    actionLink.setOnMouseEntered(ev -> actionLink.setStyle(linkStyleHover));
                    actionLink.setOnMouseExited(ev -> actionLink.setStyle(linkStyleNormal));
                    
                    actionLink.setOnMouseClicked(ev -> {
                        ev.consume();
                        if (item.isRead) {
                            NetworkNotificationService.getInstance().markAsUnread(item.id);
                        } else {
                            NetworkNotificationService.getInstance().markAsRead(item.id);
                        }
                    });
                    
                    footer.getChildren().addAll(date, separator, actionLink);
                    texts.getChildren().addAll(title, message, footer);
                    HBox.setHgrow(texts, Priority.ALWAYS);
                    
                    VBox rightBox = new VBox();
                    rightBox.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
                    rightBox.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));
                    
                    // Simple, clean blue dot for unread notifications
                    if (!item.isRead) {
                        Circle unreadDot = new Circle(4, javafx.scene.paint.Color.web("#3b82f6"));
                        rightBox.getChildren().add(unreadDot);
                    }
                    
                    container.getChildren().addAll(iconContainer, texts, rightBox);
                    
                    container.setOnMouseClicked(e -> {
                        if (!item.isRead) {
                            NetworkNotificationService.getInstance().markAsRead(item.id);
                        }
                    });
                    
                    // Context Menu for right-click toggle as a secondary option
                    javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
                    javafx.scene.control.MenuItem toggleItem = new javafx.scene.control.MenuItem(
                        item.isRead ? app.ThemeManager.get("notification.markUnread") : app.ThemeManager.get("notification.markRead")
                    );
                    toggleItem.setOnAction(ev -> {
                        if (item.isRead) {
                            NetworkNotificationService.getInstance().markAsUnread(item.id);
                        } else {
                            NetworkNotificationService.getInstance().markAsRead(item.id);
                        }
                    });
                    contextMenu.getItems().add(toggleItem);
                    container.setOnContextMenuRequested(ev -> {
                        contextMenu.show(container, ev.getScreenX(), ev.getScreenY());
                    });
                    
                    setGraphic(container);
                }
            }
        });

        updateEmptyState();
        NetworkNotificationService.getInstance().getNotifications().addListener((ListChangeListener<NotificationDto>) c -> {
            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        boolean empty = NetworkNotificationService.getInstance().getNotifications().isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        notificationListView.setVisible(!empty);
        notificationListView.setManaged(!empty);
    }

    private javafx.stage.Popup popup;

    public void setPopup(javafx.stage.Popup popup) {
        this.popup = popup;
    }

    @FXML
    private void onMarkAllRead() {
        NetworkNotificationService.getInstance().markAllAsRead();
    }
}
