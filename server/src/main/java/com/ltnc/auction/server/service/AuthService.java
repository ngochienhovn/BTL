package server.services;

import server.model.User;
import server.model.UserRole;

public class AuthService  
{
    private User[] fakeDataUser; 

    // Thuật ngữ mới parttern singleton duy trì một đối tượng duy nhất cho toàn bộ chương trình, tất cả các class khác không thể truy cập vào class intance mà phải thông qua hàm
    private static volatile AuthService instance; 

    private AuthService() 
    {
        fakeDataUser = new User[100];
    };

    public static AuthService getInstance()
    {
        if (instance == null)
        {
            synchronized (AuthService.class)
            {
                if (instance == null)
                {
                    instance = new AuthService();
                }
            }
        }

        return instance;
    }

    public record LoginResult(boolean success, String code, User user) {}; // Câu lệnh này tương đương dưới cái này của thư viện 
    // public final class LoginResult
    // {
    //     private final boolean success;
    //     private final String code;
    //     private final User user;

    //     public LoginResult(boolean success, String code, User user)
    //     {
    //         this.success = success;
    //         this.code = code;
    //         this.user = user;
    //     }

    //     public boolean success() { return success; }
    //     public String code() { return code; }
    //     public User user() { return user; }

    //     @Override
    //     public String toString()
    //     {
    //         return String.format("LoginResult:[success=%b, code=%s, user=%s]", success, code, user);
    //     }

    //     @Override
    //     public boolean equals(Object obj) {};

    //     @Override
    //     public int hashCode() {};
    // }
    public record RegisterResult(boolean success, String code, User user) {};

    // Fake logic DAO
    private User findUserByEmail(String email)
    {
        for (User user : fakeDataUser)
        {
            if (user != null && user.getEmail().equals(email))
            {
                return user;
            }
        }
        return null;
    }

    private User registerNewUser(String fullname, String email, String password)
    {
        User user = new User(fullname, email, password, UserRole.BIDDER);
        return user;
    }

    // Kết thúc fake logic DAO

    public LoginResult login(String email, String password)
    {
        User user = findUserByEmail(email); // Tìm user trong database fake
        if (user == null)
        {
            return new LoginResult(false, "USER_NOT_FOUND", null); // Nếu không tìm thấy user thì trả về kết quả lỗi
        }
        if (!user.getPasswordHash().equals(password))
        {
            return new LoginResult(false, "INVALID_PASSWORD", null); // Nếu password không khớp thì trả về kết quả lỗi
        }

        return new LoginResult(true, "LOGIN_SUCCESS", user); // Nếu tìm thấy user và password khớp thì trả về kết quả thành công
    }

    public RegisterResult register(String fullname, String email, String password)
    {
        User user = findUserByEmail(email); // Tìm user trong database fake

        if (user != null)
        {
            return new RegisterResult(false, "USER_ALREADY_EXISTS", null); // Nếu đã tồn tại user thì trả về kết quả lỗi
        }

        user = registerNewUser(fullname, email, password);
        return new RegiterResult(true, "REGISTER_SUCCESS", user); // Nếu tạo user mới thành công thì trả về kết quả thành công
    }

    public static void main(String[] args)
    {
        AuthService authService = AuthService.getInstance();

        LoginResult loginResult = authService.login("test@example.com", "password");
        System.out.println(loginResult);
        RegisterResult registerResult = authService.register("Ho Ngoc Hien", "test@example.com", "password");
        System.out.println(registerResult);
        LoginResult loginResult2 = authService.login("test@example.com", "password");
        System.out.println(loginResult2);
    }
}
