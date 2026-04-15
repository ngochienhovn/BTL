// Nếu file lúc trước là file để khởi tạo database thì đây sẽ là những phương thức tương tác với từng table của database ví dụ các phương thức
// Liên quan đến table items thì implement ở đây
package src.dao;

import model.Item;
import model.ItemFactory; // Design pattern factory để tạo item mới

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO 
{
    // Khởi tạo constructor
    public ItemDAO() {};

    // Tìm item bằng email của seller
    public List<Item> findBySeller(String email)
    {
        String query_sql = "SELECT id, seller_id, seller_email, type, name, description, starting_bid, img_url FROM items WHERE seller_email = ?";
        List<Item> items = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(query_sql);
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    items.add(MapItem(rs));
                }
            }
        } catch (SQLException e) 
        {
            System.err.println("Error finding items by seller: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return items;
    }

    public boolean insert(Item item, long sellerId)
    {
        String query_sql = "INSERT INTO items (seller_id, seller_email, type, name, description, starting_bid, img_url) VALUES (?, ?, ?, ?, ?, ?, ?)"; // Dây là câu truy vấn sql

        try (Connection conn = DBconnection.getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(query_sql); // Tạo prepared statement để thực hiện câu truy vấn sql
            stmt.setLong(1, item != null ? sellerId : null); // Lấy giá trị đầu tiên của bảng truy vấn
            stmt.setString(2, item.getSellerEmail()); // Lấy giá trị thứ hai của bảng truy vấn
            stmt.setString(3, item.getType()); // Lấy giá trị thứ ba của bảng truy vấn
            stmt.setString(4, item.getName()); // Lấy giá trị thứ tư của bảng truy vấn
            stmt.setString(5, item.getDescription()); // Lấy giá trị thứ năm của bảng truy vấn
            stmt.setDouble(6, item.getStartingBid()); // Lấy giá trị thứ sáu của bảng truy vấn
            stmt.setString(7, item.getImgUrl()); // Lấy giá trị thứ bảy của bảng truy vấn
            stmt.executeUpdate(); // Thực hiện câu truy vấn sql

            try (ResultSet keys = stmt.getGeneratedKeys()) // Lấy giá trị đầu tiên của bảng truy vấn
            {
                if (keys.next()) // Nếu có dữ liệu trả về thì lấy dữ liệu đó
                {
                    return keys.getLong(1); // Trả về id của item
                }
            }
        } catch (SQLException e)
        {
            System.err.println("Error inserting item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        return null; // Trả về null nếu thêm item thất bại
    }

    public boolean update(Item item)
    {
        String query_sql = "UPDATE items SET name = ?, description = ?, starting_bid = ?, img_url = ? WHERE id = ?";

        try (Connection conn = DBconnection.getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(query_sql);
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingBid());
            stmt.setString(4, item.getImgUrl());
            stmt.setLong(5, item.getId());
            return stmt.executeUpdate() > 0; // Trả về true nếu cập nhật item thành công
        } catch (SQLException e)
        {
            System.err.println("Error updating item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return false; // Trả về false nếu cập nhật item thất bại
    }

    public boolean delete(Item item)
    {
        String query_sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DBconnection.getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(query_sql);
            stmt.setLong(1, item.getId());
            return stmt.executeUpdate() > 0; // Trả về true nếu xóa item thành công
        } catch (SQLException e)
        {
            System.err.println("Error deleting item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return false; // Trả về false nếu xóa item thất bại
    }

    // Map item từ ResultSet sang Item object vì kiểu dữ liệu lấy từ database về khác so với dữ liệu cần dùng nên cần format lại chuẩn
    private Item MapItem(ResultSet rs) throws SQLException
    {
        long id = rs.getLong("id");
        long sellerId = rs.getLong("seller_id");
        String sellerEmail = rs.getString("seller_email");
        String type = rs.getString("type");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startingBid = rs.getDouble("starting_bid");
        String imgUrl = rs.getString("img_url");

        Item item = ItemFactory.create(type, name, description, startingBid, imgUrl, sellerEmail);
        item.setId(id);
        item.setSellerId(sellerId);

        return item;
    }

    // Done task 2: Implement ItemDAO, UserDAO
}
