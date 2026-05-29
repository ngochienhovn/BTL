package client.app.controller;


import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController 
{
    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label confirmPasswordErrorLabel;

    @FXML
    private Label signUpSuccessLabel;

    @FXML
    private ComboBox<String> signUpRoleCombo;

    @FXML
    private TextField signInEmailField;

    @FXML
    private PasswordField signInPasswordField;

    @FXML
    private Label signInEmailErrorLabel;

    @FXML
    private Label signInPasswordErrorLabel;

    @FXML
    private javafx.scene.control.Button signInSubmitButton;

    private boolean emailTouched;
    private boolean passwordTouched;
    private boolean confirmTouched;
    private boolean forceShowAllErrors;
    private final IUserService userService = InMemoryUserService.INSTANCE;

    @FXML
    private void initialize() {
        if (emailField != null) {
            emailField.textProperty().addListener((obs, ov, nv) -> {
                emailTouched = true;
                try {
                    updateSignUpValidationRealtime();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, ov, nv) -> {
                passwordTouched = true;
                try {
                    updateSignUpValidationRealtime();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, ov, nv) -> {
                confirmTouched = true;
                try {
                    updateSignUpValidationRealtime();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (signInEmailField != null) {
            signInEmailField.textProperty().addListener((obs, ov, nv) -> {
                try {
                    updateSignInEmailValidationRealtime();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            try {
                updateSignInEmailValidationRealtime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (signUpRoleCombo != null) {
            signUpRoleCombo.getItems().setAll("Bidder", "Seller");
            signUpRoleCombo.setValue("Bidder");
        }
        try {
            updateSignUpValidationRealtime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackHome() {
        showFieldMessage(signUpSuccessLabel, "Back Home (demo).", true);
    }

    @FXML
    private void onOpenSignIn() {
        showFieldMessage(signUpSuccessLabel, "Open Sign In (demo).", true);
    }

    @FXML
    private void onOpenSignUp() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/sign-up.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Stage stage = getCurrentStage();
        if (stage == null) {
            throw new IllegalStateException("Khong tim thay cua so hien tai de chuyen trang.");
        }
        stage.setScene(scene);
        stage.show();
    }

    private Stage getCurrentStage() {
        Node[] candidates = new Node[] {
                signInSubmitButton,
                signInEmailField,
                signInPasswordField,
                emailField,
                passwordField,
                confirmPasswordField,
                fullNameField
        };
        for (Node node : candidates) {
            if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
                return stage;
            }
        }
        return null;
    }

    @FXML
    private void onSubmitSignUp() throws IOException {
        if (emailField == null || passwordField == null || confirmPasswordField == null) {
            return;
        }
        forceShowAllErrors = true;
        emailTouched = true;
        passwordTouched = true;
        confirmTouched = true;
        updateSignUpValidationRealtime();

        String email = emailField.getText() != null ? emailField.getText() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";
        String confirmPassword = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        String emailError = validateEmail(email);
        String passwordError = validatePassword(password);
        String confirmError = validateConfirmPassword(password, confirmPassword);

        boolean valid = emailError.isEmpty() && passwordError.isEmpty() && confirmError.isEmpty();
        if (valid) {
            String fullName = fullNameField != null ? fullNameField.getText() : "";
            UserRole role = "Seller".equalsIgnoreCase(signUpRoleCombo != null ? signUpRoleCombo.getValue() : "Bidder")
                    ? UserRole.SELLER
                    : UserRole.BIDDER;
            RegisterResult result = userService.register(fullName, email, password, role);
            if (result.success()) {
                showFieldMessage(signUpSuccessLabel, "Đăng ký thành công.", true);
            } else if ("EMAIL_EXISTS".equals(result.code())) {
                showFieldMessage(emailErrorLabel, "Email đã tồn tại.", false);
                showFieldMessage(signUpSuccessLabel, "", true);
            }
        }
        forceShowAllErrors = false;
    }

    @FXML
    private void onSubmitSignIn() throws IOException {
        updateSignInEmailValidationRealtime();
        if (signInEmailField == null || signInPasswordField == null) {
            return;
        }

        // --- Loading state: khóa nút tránh double-click ---
        if (signInSubmitButton != null) {
            signInSubmitButton.setDisable(true);
            signInSubmitButton.setText("Signing in...");
        }

        String email = signInEmailField.getText() != null ? signInEmailField.getText() : "";
        String password = signInPasswordField.getText() != null ? signInPasswordField.getText() : "";
        String emailError = validateEmail(email);
        if (!emailError.isEmpty()) {
            showFieldMessage(signInEmailErrorLabel, emailError, false);
            showFieldMessage(signInPasswordErrorLabel, "", false);
            restoreSignInButton();
            return;
        }

        LoginResult result = userService.login(email, password);
        if (result.success()) {
            showFieldMessage(signInEmailErrorLabel, "", false);
            showFieldMessage(signInPasswordErrorLabel, "", false);
            User user = result.user();
            showFieldMessage(signInEmailErrorLabel, "Xin chao, " + user.fullName(), true);
            restoreSignInButton();
            return;
        }

        showFieldMessage(signInEmailErrorLabel, "Email hoac mat khau khong dung.", false);
        showFieldMessage(signInPasswordErrorLabel, "", false);
        restoreSignInButton();
    }

    private void restoreSignInButton() throws IOException {
        if (signInSubmitButton != null) {
            signInSubmitButton.setDisable(false);
            signInSubmitButton.setText("Sign In");
        }
    }

    private void updateSignUpValidationRealtime() throws IOException {
        if (emailField == null || passwordField == null || confirmPasswordField == null) {
            return;
        }

        String email = emailField.getText() != null ? emailField.getText() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";
        String confirmPassword = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        // If user hasn't started typing anything yet, keep it clean.
        if (email.isBlank() && password.isBlank() && confirmPassword.isBlank()) {
            showFieldMessage(emailErrorLabel, "", false);
            showFieldMessage(passwordErrorLabel, "", false);
            showFieldMessage(confirmPasswordErrorLabel, "", false);
            showFieldMessage(signUpSuccessLabel, "", true);
            return;
        }

        String emailError = validateEmail(email);
        String passwordError = validatePassword(password);
        String confirmError = validateConfirmPassword(password, confirmPassword);

        String emailMsg = (forceShowAllErrors || emailTouched) ? emailError : "";
        String passwordMsg = (forceShowAllErrors || passwordTouched) ? passwordError : "";
        String confirmMsg = (forceShowAllErrors || confirmTouched) ? confirmError : "";

        showFieldMessage(emailErrorLabel, emailMsg, false);
        showFieldMessage(passwordErrorLabel, passwordMsg, false);
        showFieldMessage(confirmPasswordErrorLabel, confirmMsg, false);

        boolean valid = emailError.isEmpty() && passwordError.isEmpty() && confirmError.isEmpty();
        // Chỉ hiện "Hợp lệ." (hoặc thông báo thành công) khi bấm Sign Up
        String successMsg = (forceShowAllErrors && valid) ? "Đăng ký thành công (demo)." : "";
        showFieldMessage(signUpSuccessLabel, successMsg, true);
    }

    private void showFieldMessage(Label label, String message, boolean isSuccess) {
        if (label == null) {
            return;
        }
        label.setText(message);
        label.getStyleClass().removeAll("success-label", "error-label");
        label.getStyleClass().add(isSuccess ? "success-label" : "error-label");
        boolean visible = message != null && !message.isBlank();
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void updateSignInEmailValidationRealtime() throws IOException {
        if (signInEmailField == null || signInEmailErrorLabel == null) {
            return;
        }

        String email = signInEmailField.getText() != null ? signInEmailField.getText() : "";
        if (email.isBlank()) {
            showFieldMessage(signInEmailErrorLabel, "", false);
            showFieldMessage(signInPasswordErrorLabel, "", false);
            return;
        }

        String error = validateEmail(email);
        showFieldMessage(signInEmailErrorLabel, error, false);
    }

    private String validateEmail(String email) throws IOException {
        if (email == null || email.isBlank()) {
            return "Email không được để trống.";
        }

        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");
        if (!emailPattern.matcher(email).matches()) {
            return "Email phải có định dạng hợp lệ (ví dụ: ten@gmail.com).";
        }
        return "";
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return "Mật khẩu không được để trống.";
        }

        if (password.length() < 8) {
            return "Mật khẩu phải dài ít nhất 8 ký tự.";
        }

        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        if (!hasSpecial) {
            return "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt.";
        }
        return "";
    }

    private String validateConfirmPassword(String password, String confirmPassword) throws IOException {
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            return "Mật khẩu xác nhận không khớp.";
        }

        return "";
    }

    enum UserRole {
        BIDDER,
        SELLER
    }

    record User(String fullName, String email, UserRole role) {}

    record RegisterResult(boolean success, String code) {}

    record LoginResult(boolean success, User user, String code) {}

    interface IUserService {
        RegisterResult register(String fullName, String email, String password, UserRole role);
        LoginResult login(String email, String password);
    }

    static final class InMemoryUserService implements IUserService {
        private static final InMemoryUserService INSTANCE = new InMemoryUserService();
        private final Map<String, String> passwordsByEmail = new HashMap<>();
        private final Map<String, User> usersByEmail = new HashMap<>();

        private InMemoryUserService() {}

        @Override
        public RegisterResult register(String fullName, String email, String password, UserRole role) {
            String key = email == null ? "" : email.trim().toLowerCase();
            if (usersByEmail.containsKey(key)) {
                return new RegisterResult(false, "EMAIL_EXISTS");
            }
            User user = new User(fullName == null ? "" : fullName.trim(), key, role);
            usersByEmail.put(key, user);
            passwordsByEmail.put(key, password == null ? "" : password);
            return new RegisterResult(true, "OK");
        }

        @Override
        public LoginResult login(String email, String password) {
            String key = email == null ? "" : email.trim().toLowerCase();
            if (!usersByEmail.containsKey(key)) {
                return new LoginResult(false, null, "NOT_FOUND");
            }
            String stored = passwordsByEmail.getOrDefault(key, "");
            if (!stored.equals(password == null ? "" : password)) {
                return new LoginResult(false, null, "INVALID_PASSWORD");
            }
            return new LoginResult(true, usersByEmail.get(key), "OK");
        }
    }
}