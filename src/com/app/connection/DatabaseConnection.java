package com.app.connection;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
 
    private static DatabaseConnection instance;
    private Connection connection;

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private DatabaseConnection() {

    }

    public void connectToDatabase() throws SQLException {
        String server = "localhost";
        String port = "3306";
        String database = "chat_application";
        String userName = "root";
        String password = "root";
        connection = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_application", userName, password);
    }
    
    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}
