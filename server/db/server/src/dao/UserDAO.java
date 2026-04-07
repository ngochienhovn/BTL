// Nếu file lúc trước là file để khởi tạo database thì đây sẽ là những phương thức tương tác với từng table của database ví dụ các phương thức
// Liên quan đến table items thì implement ở đây
package src.dao;

import model.User;

// Import DBConnection
import src.db.DBConnection;

// Import thu vien sql
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO 
{
    // Khởi tạo constructor
    public UserDAO() {};

    // Tìm user bẳng email trong database
    public User findByEmail(String email)
    {
        // Kiểm tra xem database có tồn tại không
        try (Connection conn = DBconnection.getConnection())
        {
            // Dây là câu truy vấn sql
            String query = "SELECT * FROM users WHERE email = ?";
            // Tạo prepared statement để thực hiện câu truy vấn sql
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email); // Lấy giá trị đầu tiên của bảng truy vấn
            try (ResultSet rs = stmt.executeQuery()) // Thực hiện câu truy vấn sql
            {
                if (rs.next()) // Nếu có dữ liệu trả về thì lấy dữ liệu đó
                {
                    return MapUser(rs);
                }
            } catch (SQLException e) {
                System.err.println("Error finding user by email: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Tìm user bằng id trong database
    public boolean insert(User user)
    {
        try (Connection conn = DBconnection.getConnection())
        {
            String query = "INSERT INTO users (username, fullname, password_hash, role) VALUES (?, ?, ?, ?)"; // Dây là câu truy vấn sql
            PreparedStatement stmt = conn.prepareStatement(query); // Tạo prepared statement để thực hiện câu truy vấn sql
            stmt.setString(1, user.getEmail()); // Lấy giá trị đầu tiên của bảng truy vấn
            stmt.setString(2, user.getFullname()); // Lấy giá trị thứ hai của bảng truy vấn
            stmt.setString(3, user.getPasswordHash()); // Lấy giá trị thứ ba của bảng truy vấn
            stmt.setString(4, user.getRole().name()); // Lấy giá trị thứ tư của bảng truy vấn
            stmt.executeUpdate(); // Thực hiện câu truy vấn sql

            if (ResultSet keys = stmt.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false; // Trả về false nếu tạo user thất bại
        }

        return null; // Trả về null nếu tạo user thất bại
    }

    // Map user từ ResultSet sang User object vì kiểu dữ liệu lấy từ database về khác so với dữ liệu cần dùng nên cần format lại chuẩn
    private User MapUser(ResultSet rs) throws SQLException 
    {
        long id = rs.getLong("id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        UserRole role = UserRole.valueOf(roleStr);

        User user = switch (role) {
            case ADMIN -> new Admin(id, fullName, email, passwordHash);
            case SELLER -> new Seller(id, fullName, email, passwordHash);
            case BIDDER -> new Bidder(id, fullName, email, passwordHash);
        };

        return user;
    }
}
