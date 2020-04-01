package proto_file.server;

import java.sql.*;

public class AuthService {

    private static Connection connection;
    private static Statement statement;

    static void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:network_storage.db");
        statement = connection.createStatement();
    }

    static void addNewUser(String login, String password, String directory) throws SQLException {
        String query = "INSERT INTO users (login, password, directory) VALUES (?, ?, ?);";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, login);
        ps.setInt(2, password.hashCode());
        ps.setString(3, directory);
        ps.executeUpdate();
    }

    static int deleteUser(String login, String password) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE login = ?, password = ?");
        ps.setString(1, login);
        ps.setInt(2, password.hashCode());
        return ps.executeUpdate();
    }

    static int clearTable(String nameTable) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM ?");
        ps.setString(1, nameTable);
        return ps.executeUpdate();
    }

    static String getDirectoryByLoginAndPassword(String login, String password) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT directory, password FROM users WHERE login = ?");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        int myHash = password.hashCode();
        if (rs.next()) {
            String directory = rs.getString(1);
            int dbHash = rs.getInt(2);
            if (myHash == dbHash) {
                return directory;
            }
        }
        return null;
    }

    static boolean loginIsThere(String login) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT id FROM users WHERE login = ?");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        int id = 0;
        if (rs.next()){
            id = rs.getInt("id");
        }
        return id != 0;
    }

    public static void disconnect() throws SQLException {
        connection.close();
    }

}
