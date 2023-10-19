package com.app.service;

import com.app.connection.DatabaseConnection;
import com.app.model.Model_Client;
import com.app.model.Model_Login;
import com.app.model.Model_Message;
import com.app.model.Model_Register;
import com.app.model.Model_User_Account;
import java.sql.Blob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {

    public ServiceUser() {
        this.con = DatabaseConnection.getInstance().getConnection();
    }

    public Model_Message register(Model_Register data) {
        //Check user exit
        Model_Message message = new Model_Message();
        try {
            PreparedStatement p = con.prepareStatement(CHECK_USER);
            p.setString(1, data.getUserName());
            ResultSet r = p.executeQuery();
            if (r.next()) {
                message.setAction(false);
                message.setMessage("User Already Exit");

            } else {
                message.setAction(true);
            }
            r.close();
            p.close();
            if (message.isAction()) {
                //Insert User Register
                con.setAutoCommit(false);
                p = con.prepareStatement(INSERT_USER, PreparedStatement.RETURN_GENERATED_KEYS);
                p.setString(1, data.getUserName());
                p.setString(2, data.getPassword());
                p.execute();
                r = p.getGeneratedKeys();
                r.next();
                int userID = r.getInt(1);
                r.close();
                p.close();
                //creat user account
                p = con.prepareStatement(INSERT_USER_ACCOUNT);
                p.setInt(1, userID);
                p.setString(2, data.getUserName());
                p.execute();
                p.close();
                con.commit();
                con.setAutoCommit(true);
                message.setAction(true);
                message.setMessage("OK");
                message.setData(new Model_User_Account(userID, data.getUserName(), "", null, true));
            }

        } catch (SQLException e) {
            message.setAction(false);
            message.setMessage("Server Error");
            try {
                if (con.getAutoCommit() == false) {
                    con.rollback();
                    con.setAutoCommit(true);
                }
            } catch (SQLException e1) {

            }
        }
        return message;
    }

    public Model_User_Account login(Model_Login login) throws SQLException {
        Model_User_Account data = null;
        PreparedStatement p = con.prepareStatement(LOGIN);
        p.setString(1, login.getUserName());
        p.setString(2, login.getPassword());
        ResultSet r = p.executeQuery();
        if (r.next()) {
            int userID = r.getInt(1);
            String userName = r.getString(2);
            String gender = r.getString(3);
            String image = r.getString(4);
            data = new Model_User_Account(userID, userName, gender, image, true);
        }
        r.close();
        p.close();
        return data;
    }

    public List<Model_User_Account> getUser(int exitUser) throws SQLException {
        List<Model_User_Account> list = new ArrayList<>();
        PreparedStatement p = con.prepareStatement(SELECT_USER_ACCOUNT);
        p.setInt(1, exitUser);
        ResultSet r = p.executeQuery();
        while (r.next()) {
            int userID = r.getInt(1);
            String userName = r.getString(2);
            String gender = r.getString(3);
            String image = r.getString(4);
            list.add(new Model_User_Account(userID, userName, gender, image, checkUserStatus(userID)));
        }
        r.close();
        p.close();
        return list;
    }

    public Model_User_Account getOneUser(int exitUser) throws SQLException {
        Model_User_Account user;
        PreparedStatement p = con.prepareStatement(SELECT_USER_ACCOUNT_ONE);
        p.setInt(1, exitUser);
        ResultSet r = p.executeQuery();
        r.next();
        int userID = r.getInt(1);
        String userName = r.getString(2);
        String gender = r.getString(3);
        String image = r.getString(4);
        user = new Model_User_Account(userID, userName, gender, image, checkUserStatus(userID));

        r.close();
        p.close();
        return user;
    }

    public void setUserAvata(int userID, File image) throws SQLException {
        PreparedStatement s = con.prepareStatement(RESET_USER_AVATA);
        s.setInt(1, userID);
        s.executeUpdate();
        s.close();
        try ( PreparedStatement p = con.prepareStatement(UPDATE_USER_AVATA)) {
            FileInputStream fis = new FileInputStream(image);
            p.setBinaryStream(1, fis);
            p.setInt(2, userID);
            p.executeUpdate();
            fis.close();
            p.close();
            PreparedStatement q = con.prepareStatement(UPDATE_AVATA_DONE);
            q.setInt(1, userID);
            q.executeUpdate();
            q.close();
        } catch (IOException | SQLException e) {
            System.out.println(e);
        }
    }

    private boolean checkUserStatus(int userID) {
        List<Model_Client> clients = Service.getInstance(null).getListClient();
        for (Model_Client c : clients) {
            if (c.getUser().getUserID() == userID) {
                return true;
            }
        }
        return false;
    }
    //getUserAvata
    public byte[] getUserAvata(int userID) throws SQLException{
        byte[] avatarData = null;
        PreparedStatement p = con.prepareStatement(SELECT_AVATA_USER);
        p.setInt(1, userID);
        ResultSet r = p.executeQuery();
        if (r.next()) {
                Blob avatarBlob = r.getBlob("Image");
                if (avatarBlob != null) {
                    avatarData = avatarBlob.getBytes(1, (int) avatarBlob.length());
                    avatarBlob.free();
                }
            }
        return avatarData;
    }
    //SQL
    private final String LOGIN = "select UserID,user_account.UserName,Gender,ImageString from `user` join user_account using (UserID) where `user`.UserName=BINARY(?) and `user`.`Password`=BINARY(?) and user_account.`Status`='1'";
    private final String SELECT_USER_ACCOUNT = "select UserID, UserName, Gender, ImageString from user_account where user_account. `Status`='1' and UserID<>? ";
    private final String INSERT_USER = "insert into user (`UserName`, `Password`) values (?,?)";
    private final String INSERT_USER_ACCOUNT = "insert into user_account (UserID, UserName) values (?,?)";
    private final String CHECK_USER = "select UserID from user where UserName = ? limit 1";
    private final String SELECT_USER_ACCOUNT_ONE = "select UserID, UserName, Gender, Image from user_account where user_account. `Status`='1' and UserID = ? ";
    private final String UPDATE_USER_AVATA = "update user_account set `Image`= ? where UserID=? limit 1";
    private final String RESET_USER_AVATA = "update user_account  set `Image` = ''  where UserID=? limit 1;";
    private final String UPDATE_AVATA_DONE = "update user_account set `ImageString`= '1' where UserID=? limit 1";
    private final String SELECT_AVATA_USER = "select Image from user_account where UserID = ?";
    //Instance
    private final Connection con;

}
