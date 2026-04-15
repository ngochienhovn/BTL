package app.service;

import java.util.regex.Pattern;

public class AuthService {

    // Regex email cơ bản
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");



    public static String validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "Email không được để trống";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email không hợp lệ";
        }
        return null; // null = hợp lệ
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password không được để trống";
        }
        if (password.length() < 6) {
            return "Password phải >= 6 ký tự";
        }
        return null;
    }

    public static String validateConfirmPassword(String password, String confirm) {
        if (!password.equals(confirm)) {
            return "Mật khẩu không khớp";
        }
        return null;
    }

    public static String validateFullName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Tên không được để trống";
        }
        return null;
    }

    public static String validateRole(Object role) {
        if (role == null) {
            return "Vui lòng chọn role";
        }
        return null;
    }



    public static boolean register(String fullName, String email, String password, String role) {
        // TODO: lưu DB
        System.out.println("Đăng ký user: - AuthService.java:58" + email);
        return true;
    }
}