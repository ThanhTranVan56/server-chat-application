package com.app.service;

import com.app.connection.DatabaseConnection;
import com.app.model.Model_File;
import com.app.model.Model_Load_Data;
import com.app.model.Model_Receive_File;
import com.app.model.Model_Receive_Image;
import com.app.model.Model_Send_Message;
import com.app.swing.blurhash.BlurHash;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.util.Date;
import javax.imageio.ImageIO;

public class ServiceDataSave {

    private ServiceFile serviceFile;

    public ServiceDataSave() {
        this.con = DatabaseConnection.getInstance().getConnection();
        serviceFile = new ServiceFile();
    }

    public void dataSave(Model_Send_Message data) throws SQLException {
        try {
            PreparedStatement p = con.prepareStatement(INSERT_DATA, PreparedStatement.RETURN_GENERATED_KEYS);
            p.setInt(1, data.getMessageType());
            p.setInt(2, data.getFromUserID());
            p.setInt(3, data.getToUserID());
            p.setString(4, data.getText());
            p.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            p.execute();
        } catch (SQLException e) {
            System.out.print(e);
        }

    }

    public List<Model_Load_Data> getData(int toID, int uID) throws SQLException, IOException {
        try {
            List<Model_Load_Data> list = new ArrayList<>();
            PreparedStatement p = con.prepareStatement(GET_DATA);
            p.setInt(1, toID);
            p.setInt(2, uID);
            p.setInt(3, uID);
            p.setInt(4, toID);
            ResultSet r = p.executeQuery();
            while (r.next()) {
                int dataType = r.getInt(1);
                int fromUID = r.getInt(2);
                int toUID = r.getInt(3);
                String content = r.getString(4);
                Timestamp dateTime = r.getTimestamp(5);
                if (dataType == 4) {
                    Model_Receive_Image dataImage = new Model_Receive_Image();
                    dataImage.setFileID(Integer.parseInt(content));
                    //System.err.println("roi" + dataImage.getFileID());
                    setdataImage(dataImage);
                    list.add(new Model_Load_Data(dataType, fromUID, toUID, content,dateTime , dataImage,null));
                } else if(dataType == 3) {
                    Model_Receive_File dataFile = new Model_Receive_File();
                    dataFile.setFileID(Integer.parseInt(content));
                    getDataFile(dataFile);
                    System.err.println("roi" + dataFile.getFileID() + "size : " + dataFile.getFileSize());
                    list.add(new Model_Load_Data(dataType, fromUID, toUID, content,dateTime, null,dataFile));
                } else{
                    list.add(new Model_Load_Data(dataType, fromUID, toUID, content,dateTime, null,null));
                }
            }
            r.close();
            p.close();
            return list;
        } catch (SQLException e) {
            System.out.print(e);
            return null;
        }
    }
    
    public void getDataFile(Model_Receive_File dataFile) throws SQLException{
        PreparedStatement p = con.prepareStatement(GET_FILE_DATA);
        p.setInt(1, dataFile.getFileID());
        ResultSet r =p.executeQuery();
        r.next(); 
        String fileName = r.getString(1);
        String fileExtension = r.getString(2);
        dataFile.setFileExtension(fileName+fileExtension);
        r.close();
        p.close();
    }
    
    public Model_Receive_Image setdataImage(Model_Receive_Image dataImage) throws IOException, SQLException {
        try {
            Model_File file = serviceFile.getFile(dataImage.getFileID());
            //System.out.println(file.getFileExtension());
            setFileImage(new File(PATH_FILE + dataImage.getFileID() + "@" +file.getFileName() + file.getFileExtension()), dataImage);
            return dataImage;
        } catch (IOException | SQLException e) {
            System.out.println(e);
            return null;
        }
    }

    private void setFileImage(File file, Model_Receive_Image dataImage) throws IOException {
        BufferedImage img = ImageIO.read(file);
        Dimension size = getAutoSize(new Dimension(img.getWidth(), img.getHeight()), new Dimension(200, 200));
        // Convert image to small size
        BufferedImage newImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(img, 0, 0, size.width, size.height, null);
        String blurhash = BlurHash.encode(newImage);
        dataImage.setWidth(size.width);
        dataImage.setHeight(size.height);
        dataImage.setImage(blurhash);
    }

    private Dimension getAutoSize(Dimension fromSize, Dimension toSize) {
        int w = toSize.width;
        int h = toSize.height;
        int iw = fromSize.width;
        int ih = fromSize.height;
        double xScale = (double) w / iw;
        double yScale = (double) h / ih;
        double scale = Math.min(xScale, yScale);
        int width = (int) (scale * iw);
        int height = (int) (scale * ih);
        return new Dimension(width, height);
    }
    //SQL
    private final String PATH_FILE = "server_data/";
    private final String INSERT_DATA = "insert into datasave (DataType, FromUsID, ToUsID, Content, Time) values (?,?,?,?,?)";
    //private final String GET_DATAs = "SELECT DataType,FromUsID,ToUsID,Content FROM `datasave` WHERE `FromUsID` = ? OR `ToUsID` = ? ORDER BY `DataID` ASC"; LIMIT 10 :!!!!
    private final String GET_DATA = "SELECT DataType, FromUsID, ToUsID, Content, Time FROM (SELECT * FROM `datasave` WHERE (`FromUsID` = ? and `ToUsID` = ?) or (`FromUsID` = ? and `ToUsID` = ?) ORDER BY `DataID` DESC LIMIT 10) AS subquery ORDER BY `DataID` ASC;";
    private final String GET_FILE_DATA = "select FileName,FileExtension from files where FileID=? limit 1";
    //Instance
    private final Connection con;

}
