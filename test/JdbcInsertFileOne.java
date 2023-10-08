import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
 
public class JdbcInsertFileOne {
 
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/chat_application";
        String user = "root";
        String password = "root";
 
        String filePath = "C:\\Users\\admin\\Desktop\\DACN_Web\\bandicam 2023-09-21 10-45-48-247.mp4";
 
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
//            String querySetLimit = "SET GLOBAL max_allowed_packet=170685450 ;";  // 10 MB
//            Statement stSetLimit = conn.createStatement();
//            stSetLimit.execute(querySetLimit);
            
            String sql = "INSERT INTO person (first_name, last_name, photo) values (?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, "Tom");
            statement.setString(2, "Eagar");
            InputStream inputStream = new FileInputStream(new File(filePath));
 
            statement.setBlob(3, inputStream);
 
            int row = statement.executeUpdate();
            if (row > 0) {
                System.out.println("A contact was inserted with photo image.");
            }
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}