@FXML
public void initialize() {
    loadAuctions();
}

private void loadAuctions() {
    // Fake list
    for (int i = 1; i <= 5; i++) {
        Button btn = new Button("Auction " + i);

        btn.setOnAction(e -> openDetail("Auction " + i));

        auctionContainer.getChildren().add(btn);
    }
}

private void openDetail(String title) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auction-detail.fxml"));
        Parent root = loader.load();

        AuctionDetailController controller = loader.getController();
        controller.setAuction(title, 300, "OPEN");

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle("Auction Detail");
        stage.show();

    } catch (Exception e) {
        e.printStackTrace();
    }