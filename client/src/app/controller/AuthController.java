package app.controller;

import app.MainApp;
import app.ThemeManager;
import app.model.User;
import app.model.UserRole;
import app.service.IUserService;
import app.service.RegisterResult;
import app.service.LoginResult;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {
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
    private final IUserService userService = MainApp.getUserService();

    @FXML
    private void initialize() {
        // Only wires up on Sign Up page (fields are null on Sign In page)
        if (emailField != null) {
            emailField.textProperty().addListener((obs, ov, nv) -> {
                emailTouched = true;
                updateSignUpValidationRealtime();
            });
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, ov, nv) -> {
                passwordTouched = true;
                updateSignUpValidationRealtime();
            });
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, ov, nv) -> {
                confirmTouched = true;
                updateSignUpValidationRealtime();
            });
        }
        if (signInEmailField != null) {
            signInEmailField.textProperty().addListener((obs, ov, nv) -> updateSignInEmailValidationRealtime());
            updateSignInEmailValidationRealtime();
        }
        if (signUpRoleCombo != null) {
            signUpRoleCombo.getItems().setAll(ThemeManager.get("user.role.bidder"), ThemeManager.get("user.role.seller"));
            signUpRoleCombo.setValue(ThemeManager.get("user.role.bidder"));
        }
        updateSignUpValidationRealtime();
    }

    @FXML
    private void onBackHome() {
        MainApp.showHomePage();
    }

    @FXML
    private void onOpenSignIn() {
        MainApp.showSignInPage();
    }

    @FXML
    private void onOpenSignUp() {
        MainApp.showSignUpPage();
    }

    @FXML
    private void onSubmitSignUp() {
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
            UserRole role = ThemeManager.get("user.role.seller").equalsIgnoreCase(
                    signUpRoleCombo != null ? signUpRoleCombo.getValue() : "")
                    ? UserRole.SELLER
                    : UserRole.BIDDER;
                    
            app.service.LoadingService.getInstance().show();
            
            new Thread(() -> {
                RegisterResult result = userService.register(fullName, email, password, role);
                javafx.application.Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (result.success()) {
                        showFieldMessage(signUpSuccessLabel, ThemeManager.get("signUp.success"), true);
                    } else {
                        if ("EMAIL_EXISTS".equals(result.code())) {
                            showFieldMessage(emailErrorLabel, ThemeManager.get("signUp.error.emailExists"), false);
                            showFieldMessage(signUpSuccessLabel, "", true);
                        } else if ("FORBIDDEN_ROLE".equals(result.code())) {
                            showFieldMessage(signUpSuccessLabel, "Role not allowed for public registration", false);
                        } else if ("NO_RESPONSE".equals(result.code())) {
                            showFieldMessage(signUpSuccessLabel, "Cannot connect to server. Please ensure the server is running.", false);
                        } else {
                            showFieldMessage(signUpSuccessLabel, "Registration failed: " + result.code(), false);
                        }
                    }
                });
            }).start();
        }
        forceShowAllErrors = false;
    }

    @FXML
    private void onSubmitSignIn() {
        updateSignInEmailValidationRealtime();
        if (signInEmailField == null || signInPasswordField == null) {
            return;
        }

        // --- Loading state: khóa nút tránh double-click ---
        if (signInSubmitButton != null) {
            signInSubmitButton.setDisable(true);
            signInSubmitButton.setText(app.ThemeManager.get("signIn.loading"));
        }
        app.service.LoadingService.getInstance().show();

        String email = signInEmailField.getText() != null ? signInEmailField.getText() : "";
        String password = signInPasswordField.getText() != null ? signInPasswordField.getText() : "";
        String emailError = validateEmail(email);
        if (!emailError.isEmpty()) {
            showFieldMessage(signInEmailErrorLabel, emailError, false);
            showFieldMessage(signInPasswordErrorLabel, "", false);
            restoreSignInButton();
            app.service.LoadingService.getInstance().hide();
            return;
        }

        LoginResult result = userService.login(email, password);
        app.service.LoadingService.getInstance().hide();
        if (result.success()) {
            showFieldMessage(signInEmailErrorLabel, "", false);
            showFieldMessage(signInPasswordErrorLabel, "", false);
            User user = result.user();
            MainApp.completeLogin(user);
            MainApp.redirectAfterLoginIfNeeded();
            return;
        }

        showFieldMessage(signInEmailErrorLabel, app.ThemeManager.get("signIn.error.credentials"), false);
        showFieldMessage(signInPasswordErrorLabel, "", false);
        restoreSignInButton();
    }

    private void restoreSignInButton() {
        if (signInSubmitButton != null) {
            signInSubmitButton.setDisable(false);
            signInSubmitButton.setText(app.ThemeManager.get("signIn.button"));
        }
    }

    private void updateSignUpValidationRealtime() {
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
        // Clear success/error message on the main label when user is typing, unless forceShowAllErrors is true
        if (!forceShowAllErrors) {
            showFieldMessage(signUpSuccessLabel, "", true);
        }
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

    private void updateSignInEmailValidationRealtime() {
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

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return ThemeManager.get("error.email.empty");
        }

        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        if (!emailPattern.matcher(email).matches()) {
            return ThemeManager.get("error.email.invalid");
        }
        return "";
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return ThemeManager.get("error.password.empty");
        }

        if (password.length() < 8) {
            return ThemeManager.get("error.password.short");
        }

        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        if (!hasSpecial) {
            return ThemeManager.get("error.password.special");
        }
        return "";
    }

    private String validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            return ThemeManager.get("error.confirm.mismatch");
        }

        return "";
    }
}
