package app;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Singleton quản lý Dark Mode và i18n cho toàn bộ ứng dụng.
 */
public class ThemeManager {

    public enum AppTheme { LIGHT, DARK }
    public enum AppLang { EN, VI }

    private static AppTheme currentTheme = AppTheme.LIGHT;
    private static AppLang  currentLang  = AppLang.EN;

    private static final String BUNDLE_BASE = "resources/i18n/messages";

    private ThemeManager() {}

    // ------------------------------------------------------------------ Theme

    public static AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public static void applyInitialTheme() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    public static void toggleTheme() {
        if (currentTheme == AppTheme.LIGHT) {
            currentTheme = AppTheme.DARK;
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            currentTheme = AppTheme.LIGHT;
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }

    public static boolean isDark() {
        return currentTheme == AppTheme.DARK;
    }

    // ------------------------------------------------------------------ i18n

    public static AppLang getCurrentLang() {
        return currentLang;
    }

    public static void toggleLang() {
        currentLang = (currentLang == AppLang.EN) ? AppLang.VI : AppLang.EN;
    }

    public static ResourceBundle getBundle() {
        Locale locale = (currentLang == AppLang.VI) ? new Locale("vi") : Locale.ENGLISH;
        return ResourceBundle.getBundle(BUNDLE_BASE, locale);
    }

    public static String get(String key) {
        try {
            return getBundle().getString(key);
        } catch (Exception e) {
            return key; // fallback: trả về chính key nếu không tìm thấy
        }
    }
}
