package kurofin.dao;

import kurofin.model.User;

import java.sql.*;

/**
 * Data Access Object untuk entitas User.
 */
public class UserDAO {

    public User getDefaultUser() throws SQLException {
        String sql = "SELECT id_user, nama_pengguna, tanggal_dibuat FROM user ORDER BY id_user ASC LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new User(
                        rs.getInt("id_user"),
                        rs.getString("nama_pengguna"),
                        rs.getString("tanggal_dibuat")
                );
            }
        }
        return null;
    }

    public boolean updateUsername(int idUser, String newName) throws SQLException {
        String sql = "UPDATE user SET nama_pengguna = ? WHERE id_user = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, idUser);
            return ps.executeUpdate() > 0;
        }
    }
}
