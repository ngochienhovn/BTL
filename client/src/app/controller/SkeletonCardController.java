package app.controller;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class SkeletonCardController {
    @FXML
    private Region imageBone;

    @FXML
    public void initialize() {
        // Simple opacity pulse to simulate shimmer
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(800), imageBone.getParent());
        fade.setFromValue(0.4);
        fade.setToValue(0.8);
        fade.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }
}
