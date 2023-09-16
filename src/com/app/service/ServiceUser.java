package com.app.service;

import com.app.connection.DatabaseConnection;
import com.app.model.Model_Message;
import com.app.model.Model_Register;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;

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
           
            if (message.isAction()) {
                //Insert User Register
                p = con.prepareStatement(INSERT_USER);
                p.setString(1, data.getUserName());
                p.setString(2, data.getPassword());
                p.execute();
                p.close();
                message.setAction(true);
                message.setMessage("OK");
            }
            r.close();
            p.close();
        } catch (SQLException e) {
            message.setAction(false);
            message.setMessage("Server Error");
        }
        return message;
    }
    //SQL
    private final String INSERT_USER = "insert into user (`UserName`, `Password`) values (?,?)";
    private final String CHECK_USER = "select UserID from user where UserName = ? limit 1";
    //Instance
    private final Connection con;

}
