package app.service;

import app.model.User;
import app.model.UserRole;
import java.util.Collection;
import java.util.List;

public interface IUserService {
    RegisterResult register(String fullName, String email, String password, UserRole role);
    LoginResult login(String email, String password);
    void logout();
    User getCurrentUser();
    boolean isLoggedIn();
    Collection<User> getAllUsers();
    List<User> getAllUsersList();
    String deleteUser(String email);
    boolean updateUser(String originalEmail, String fullName, String newPassword, UserRole role);
}
