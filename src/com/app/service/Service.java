package com.app.service;

import com.app.apps.MessageType;
import com.app.model.Model_Client;
import com.app.model.Model_File;
import com.app.model.Model_File_Receiver;
import com.app.model.Model_Load_Data;
import com.app.model.Model_Login;
import com.app.model.Model_Message;
import com.app.model.Model_Package_Sender;
import com.app.model.Model_Receive_File;
import com.app.model.Model_Receive_Image;
import com.app.model.Model_Receive_Message;
import com.app.model.Model_Register;
import com.app.model.Model_Reques_File;
import com.app.model.Model_Send_Message;
import com.app.model.Model_User_Account;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;

public class Service {

    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private ServiceFile serviceFile;
    private ServiceDataSave serviceDataSave;
    private List<Model_Client> listClient;
    private JTextArea textArea;
    private final int PORT_NUMBER = 9999;

    public static Service getInstance(JTextArea textArea) {
        if (instance == null) {
            instance = new Service(textArea);
        }
        return instance;
    }

    private Service(JTextArea textArea) {
        this.textArea = textArea;
        serviceUser = new ServiceUser();
        serviceFile = new ServiceFile();
        listClient = new ArrayList<>();
        serviceDataSave = new ServiceDataSave();
    }

    public void startServer() {
        Configuration config = new Configuration();
        config.setPort(PORT_NUMBER);
        server = new SocketIOServer(config);
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient sioc) {
                textArea.append("One client connected \n");
            }
        });
        server.addEventListener("register", Model_Register.class, new DataListener<Model_Register>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Register t, AckRequest ar) throws Exception {
                Model_Message message = serviceUser.register(t);
                ar.sendAckData(message.isAction(), message.getMessage(), message.getData());
                if (message.isAction()) {
                    textArea.append("User has Register : " + t.getUserName() + " Pass :" + t.getPassword() + "\n");
                    server.getBroadcastOperations().sendEvent("list_user", (Model_User_Account) message.getData());
                    addClient(sioc, (Model_User_Account) message.getData());
                }
            }
        });
        server.addEventListener("login", Model_Login.class, new DataListener<Model_Login>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Login t, AckRequest ar) throws Exception {
                Model_User_Account login = serviceUser.login(t);
                if (login != null) {
                    ar.sendAckData(true, login);
                    addClient(sioc, login);
                    userConnect(login.getUserID());
                } else {
                    ar.sendAckData(false);
                }
            }

        });
        server.addEventListener("list_user", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient sioc, Integer userID, AckRequest ar) throws Exception {
                try {
                    List<Model_User_Account> list = serviceUser.getUser(userID);
                    sioc.sendEvent("list_user", list.toArray());
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
        });

        server.addEventListener("send_to_user", Model_Send_Message.class, new DataListener<Model_Send_Message>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Send_Message t, AckRequest ar) throws Exception {
                sendToClient(t, ar);
            }
        });

        server.addEventListener("send_file", Model_Package_Sender.class, new DataListener<Model_Package_Sender>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Package_Sender t, AckRequest ar) throws Exception {
                try {
                    serviceFile.receiveFile(t);
                    if (t.isFinish()) {
                        ar.sendAckData(true);
                        Model_File_Receiver message = serviceFile.setColseFile(t.getFileID());
                        //System.out.println("getFromUserID: " + message.getMessage().getFromUserID() + " getToUserID: " + message.getMessage().getToUserID() + " getText: " + message.getMessage().getText() + " getMessageType: " + message.getMessage().getMessageType());
                        if (message.getMessage().getMessageType() == MessageType.IMAGE.getValue()) {
                            Model_Receive_Image dataImage = new Model_Receive_Image();
                            dataImage.setFileID(t.getFileID());
                            Model_Send_Message messages = serviceFile.closeFileImage(dataImage);
                            sendTempFileImageToClient(messages, dataImage);
                            //System.out.println(messages.getMessageType() + "   :  " + messages.getFromUserID());
                        }else if(message.getMessage().getMessageType() == MessageType.FILE.getValue()){
                           // System.out.println("đây r");
                            Model_Receive_File dataFile = new Model_Receive_File();
                            dataFile.setFileID(t.getFileID());
                            Model_Send_Message messages = serviceFile.closeFile(dataFile);
                            sendTempFileToClient(messages, dataFile);
                            //System.out.println(messages.getMessageType() + "   :  " + messages.getFromUserID());
                        }
                    } else {
                        ar.sendAckData(true);
                    }
                } catch (IOException | SQLException e) {
                    ar.sendAckData(false);
                    e.printStackTrace();
                }
            }
        });

        server.addEventListener("get_file", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient sioc, Integer t, AckRequest ar) throws Exception {
                Model_File file = serviceFile.initFile(t);
                long fileSize = serviceFile.getFileSize(t);
                ar.sendAckData(file.getFileName(),file.getFileExtension(), fileSize);
            }
        });

        server.addEventListener("reques_file", Model_Reques_File.class, new DataListener<Model_Reques_File>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Reques_File t, AckRequest ar) throws Exception {
                byte[] data = serviceFile.getFileDate(t.getCurrentLength(), t.getFileID());
                if (data != null) {
                    ar.sendAckData(data);
                } else {
                    ar.sendAckData();
                }
            }
        });

        server.addEventListener("list_data_user", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient sioc, Integer userID, AckRequest ar) throws Exception {
                try {
                    List<Model_Load_Data> list = serviceDataSave.getData(userID);
                    sioc.sendEvent("list_data_user", list.toArray());
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }

        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient sioc) {
                int userID = removeClient(sioc);
                if (userID != 0) {
                    //removed
                    userDisconnect(userID);
                }
            }

        });
        server.start();
        textArea.append("Server has start on port : " + PORT_NUMBER + "\n");
    }

    private void userConnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, true);
    }

    private void userDisconnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, false);
    }

    private void addClient(SocketIOClient client, Model_User_Account user) {
        listClient.add(new Model_Client(client, user));
    }

    private void sendToClient(Model_Send_Message data, AckRequest ar) throws SQLException {
        if (data.getMessageType() == MessageType.IMAGE.getValue() || data.getMessageType() == MessageType.FILE.getValue()) {
            try {
                Model_File file = serviceFile.addFileReceiver(data.getFileName(),data.getFileExtension());
                serviceFile.initFile(file, data);
                ar.sendAckData(file.getFileID());
                Model_Send_Message datafile = new Model_Send_Message();
                datafile.setFromUserID(data.getFromUserID());
                datafile.setToUserID(data.getToUserID());
                datafile.setMessageType(data.getMessageType());
                datafile.setText(String.valueOf(file.getFileID()));
                serviceDataSave.dataSave(datafile);
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }

        } else {
            serviceDataSave.dataSave(data);
            for (Model_Client c : listClient) {
                if (c.getUser().getUserID() == data.getToUserID()) {
                    c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), null,null));
                    break;
                }
            }
        }
    }

    private void sendTempFileImageToClient(Model_Send_Message data, Model_Receive_Image dataImage) {
        for (Model_Client c : listClient) {
            if (c.getUser().getUserID() == data.getToUserID()) {
                //System.out.println("ID : " + dataImage.getFileID() + "he: " + dataImage.getHeight() + "wi: " + dataImage.getWidth());
                c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), dataImage,  null));
                break;
            }
        }
    }
    
    private void sendTempFileToClient(Model_Send_Message data, Model_Receive_File dataFile) {
        for (Model_Client c : listClient) {
            if (c.getUser().getUserID() == data.getToUserID()) {
                //System.out.println("ID : " + dataFile.getFileID() + "he: " + dataFile.getFileExtension() + "wi: " + dataFile.getFileSize());
                c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), null,  dataFile));
                break;
            }
        }
    }

    public int removeClient(SocketIOClient client) {
        for (Model_Client d : listClient) {
            if (d.getClient() == client) {
                listClient.remove(d);
                return d.getUser().getUserID();
            }
        }
        return 0;
    }

    public List<Model_Client> getListClient() {
        return listClient;
    }

}
