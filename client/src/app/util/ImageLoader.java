package app.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static Image placeholderImage;
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final File CACHE_DIR;
    private static final ExecutorService diskExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    static {
        String userHome = System.getProperty("user.home");
        CACHE_DIR = new File(userHome, ".bidmaster/cache/images");
        try {
            if (!CACHE_DIR.exists()) {
                CACHE_DIR.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }
    }

    public static Image getPlaceholderImage() {
        if (placeholderImage == null) {
            try {
                URL resource = ImageLoader.class.getResource("/resources/images/placeholder.png");
                if (resource != null) {
                    placeholderImage = new Image(resource.toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Failed to load local placeholder image: " + e.getMessage());
            }
        }
        return placeholderImage;
    }

    public static void loadImage(ImageView imageView, String urlStr, double width, double height, boolean preserveRatio) {
        if (imageView == null) return;
        
        Image placeholder = getPlaceholderImage();
        
        if (urlStr == null || urlStr.trim().isEmpty()) {
            imageView.setImage(placeholder);
            return;
        }

        String cacheKey = urlStr + "_" + width + "_" + height + "_" + preserveRatio;
        Image cachedImg = imageCache.get(cacheKey);
        
        if (cachedImg != null) {
            if (cachedImg.isError()) {
                imageView.setImage(placeholder);
            } else {
                imageView.setImage(cachedImg);
            }
            return;
        }

        // Generate cache key and skeleton if it is a network URL
        boolean isNetworkUrl = urlStr.startsWith("http://") || urlStr.startsWith("https://");
        
        if (!isNetworkUrl) {
            // Load local resource directly using JFX background loading
            try {
                Image img = new Image(urlStr, width, height, preserveRatio, true, true);
                imageView.setImage(img);
                img.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        imageView.setImage(placeholder);
                    }
                });
                imageCache.put(cacheKey, img);
            } catch (Exception e) {
                imageView.setImage(placeholder);
            }
            return;
        }

        // For network URLs, apply Skeleton Loading and Disk Cache
        final Rectangle skeleton = createSkeleton(imageView, width, height);

        diskExecutor.submit(() -> {
            try {
                String diskFileName = getCacheFileName(urlStr);
                File cachedFile = new File(CACHE_DIR, diskFileName);

                if (!cachedFile.exists() || cachedFile.length() == 0) {
                    // Download file to disk cache
                    downloadAndCache(urlStr, cachedFile);
                }

                if (cachedFile.exists() && cachedFile.length() > 0) {
                    String fileUrl = cachedFile.toURI().toString();
                    
                    // Decode image on the BACKGROUND thread!
                    // This prevents blocking the UI thread (lag) when loading many cached images.
                    Image img = new Image(fileUrl, width, height, preserveRatio, true);
                    
                    Platform.runLater(() -> {
                        if (img.isError()) {
                            imageView.setImage(placeholder);
                        } else {
                            imageView.setImage(img);
                            imageCache.put(cacheKey, img); // Keep it in memory cache
                        }
                        removeSkeleton(imageView, skeleton);
                    });
                } else {
                    throw new Exception("Cache file empty or invalid after download");
                }
            } catch (Exception e) {
                System.out.println("Image offline/unavailable: " + e.getMessage() + " (using placeholder)");
                Platform.runLater(() -> {
                    imageView.setImage(placeholder);
                    removeSkeleton(imageView, skeleton);
                });
            }
        });
    }

    public static void loadImage(ImageView imageView, String urlStr) {
        loadImage(imageView, urlStr, 0, 0, true);
    }

    private static String getCacheFileName(String urlStr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(urlStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            // Append file extension if present in the original url, to maintain file headers correctly
            String extension = "";
            int lastDot = urlStr.lastIndexOf('.');
            if (lastDot > 0 && urlStr.length() - lastDot <= 5) {
                extension = urlStr.substring(lastDot);
                if (extension.contains("?") || extension.contains("&")) {
                    extension = extension.split("[?&]")[0];
                }
            }
            return hexString.toString() + extension;
        } catch (Exception e) {
            return String.valueOf(urlStr.hashCode());
        }
    }

    private static void downloadAndCache(String urlStr, File cacheFile) throws Exception {
        File tempFile = File.createTempFile("bm_cache_", ".tmp", CACHE_DIR);
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error code: " + responseCode);
            }
            
            try (InputStream in = conn.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            if (!tempFile.renameTo(cacheFile)) {
                if (cacheFile.exists()) {
                    return; // already cache written
                }
                throw new Exception("Failed to rename temp file to cached file");
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private static Rectangle createSkeleton(ImageView imageView, double width, double height) {
        double w = width > 0 ? width : (imageView.getFitWidth() > 0 ? imageView.getFitWidth() : 284);
        double h = height > 0 ? height : (imageView.getFitHeight() > 0 ? imageView.getFitHeight() : 190);
        
        Rectangle skeleton = new Rectangle(w, h);
        skeleton.setArcWidth(16);
        skeleton.setArcHeight(16);
        // Modern glass skeleton gradient
        skeleton.setStyle("-fx-fill: linear-gradient(to right, -color-bg-subtle, derive(-color-bg-subtle, -8%), -color-bg-subtle);");
        
        FadeTransition pulse = new FadeTransition(Duration.millis(800), skeleton);
        pulse.setFromValue(0.4);
        pulse.setToValue(1.0);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
        
        Platform.runLater(() -> {
            Pane parent = (Pane) imageView.getParent();
            if (parent != null) {
                // If there's already a skeleton, remove it first
                parent.getChildren().removeIf(node -> "image-skeleton".equals(node.getId()));
                skeleton.setId("image-skeleton");
                
                int idx = parent.getChildren().indexOf(imageView);
                if (idx >= 0) {
                    parent.getChildren().add(idx + 1, skeleton);
                } else {
                    parent.getChildren().add(skeleton);
                }
            }
        });
        
        return skeleton;
    }

    private static void removeSkeleton(ImageView imageView, Rectangle skeleton) {
        if (skeleton == null) return;
        Platform.runLater(() -> {
            Pane parent = (Pane) imageView.getParent();
            if (parent != null && parent.getChildren().contains(skeleton)) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(350), skeleton);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> parent.getChildren().remove(skeleton));
                fadeOut.play();
            }
        });
    }
}

