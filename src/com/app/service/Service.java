package com.app.service;

import com.app.apps.MessageType;
import com.app.model.Model_Client;
import com.app.model.Model_File;
import com.app.model.Model_File_Receiver;
import com.app.model.Model_Group;
import com.app.model.Model_Load_Data;
import com.app.model.Model_Login;
import com.app.model.Model_Message;
import com.app.model.Model_Package_Sender;
import com.app.model.Model_Receive_File;
import com.app.model.Model_Receive_Image;
import com.app.model.Model_Receive_Image_Group;
import com.app.model.Model_Receive_Message;
import com.app.model.Model_Receive_Message_Group;
import com.app.model.Model_Register;
import com.app.model.Model_Register_Group;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTextArea;

public class Service {

    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private ServiceFile serviceFile;
    private ServiceDataSave serviceDataSave;
    private ServiceGroup serviceGroup;
    private List<Model_Client> listClient;
    private JTextArea textArea;
    private final String PATH_FILE = "server_data/";
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
        serviceGroup = new ServiceGroup();
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

        //get_username_pass
        server.addEventListener("get_username_pass", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient sioc, Integer t, AckRequest ar) throws Exception {
                String re = serviceUser.getUserNamePass(t);
                ar.sendAckData(re);
            }

        });
        //update_username_pass
        server.addEventListener("update_username_pass", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient sioc, String message, AckRequest ar) throws Exception {
                String[] parts = message.split("@");
                boolean re = serviceUser.updateUser(Integer.parseInt(parts[0]), parts[1], parts[2]);
                ar.sendAckData(re);
            }

        });
        server.addEventListener("user_join_group", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String groupName, AckRequest ackRequest) throws Exception {
                int adminID = serviceGroup.getAdmin(groupName);
                String uName = "";
                boolean isAction = false;
                String messageRe = "Đang chờ phản hồi từ Admin";
                ackRequest.sendAckData(isAction, messageRe);
                for (Model_Client d : listClient) {
                    if (d.getClient() == socketIOClient) {
                        uName = d.getUser().getUserName();
                        break;
                    }
                }
                boolean found = false;
                for (Model_Client c : listClient) {
                    if (c.getUser().getUserID() == adminID) {
                        c.getClient().sendEvent("request_join_group", uName, groupName);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String messages = "Admin did not respond!";
                    socketIOClient.sendEvent("send_join_group", messages);
                }
            }
        });

        server.addEventListener("send_join_group", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String message, AckRequest ackRequest) throws Exception {
                String[] parts = message.split("@");
                if ("OK".equals(parts[0])) {
                    for (Model_Client d : listClient) {
                        if (d.getUser().getUserName().equals(parts[1])) {
                            d.getClient().joinRoom(parts[2]);
                            String messages = "Welcome to the group";
                            d.getClient().sendEvent("send_join_group", messages);
                            int groupID = serviceGroup.getGroupID(parts[2]);
                            serviceGroup.setMemberGroup(groupID, d.getUser().getUserID());
                            break;
                        }
                    }

                } else {
                    for (Model_Client d : listClient) {
                        if (d.getUser().getUserName().equals(parts[1])) {
                            String messages = "So sorry";
                            d.getClient().sendEvent("send_join_group", messages);
                            break;
                        }
                    }
                }
                System.out.println(message);
            }
        });

        //registerGroup
        server.addEventListener("registerGroup", Model_Register_Group.class, new DataListener<Model_Register_Group>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Register_Group t, AckRequest ar) throws Exception {
                Model_Message message = serviceGroup.registerGroup(t);
                //group status
                groupAdd(serviceGroup.getGroup(t.getGroupName()));
                ar.sendAckData(message.isAction(), message.getMessage(), message.getData());
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

        server.addEventListener("list_group", Void.class, new DataListener<Void>() {
            @Override
            public void onData(SocketIOClient sioc, Void data, AckRequest ar) throws Exception {
                try {
                    List<Model_Group> list = serviceGroup.getGroups();
                    sioc.sendEvent("list_group", list.toArray());
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
        });
        //check_group_online
        server.addEventListener("check_group_online", Void.class, new DataListener<Void>() {
            @Override
            public void onData(SocketIOClient sioc, Void data, AckRequest ar) throws Exception {
                try {
                    List<Model_Group> list = serviceGroup.getGroups();
                    List<Integer> listID = new ArrayList<>();
                    for (Model_Group l : list) {
                        for (Model_Client d : listClient) {
                            if (d.getUser().getUserID() == l.getAdminID()) {
                                listID.add(l.getGroupID());
                            }
                        }
                    }
                    sioc.sendEvent("check_group_online", listID.toArray());
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
        });
        //check_group_member
        server.addEventListener("check_group_member", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient socketIOClient, Integer groupID, AckRequest ackRequest) throws Exception {
                boolean found = false;
                boolean isaction = false;
                List<Model_Group> list = serviceGroup.getGroups();
                for (Model_Client d : listClient) {
                    if (d.getClient() == socketIOClient) {
                        for (Model_Group l : list) {
                            if (l.getAdminID() == d.getUser().getUserID()) {
                                isaction = true;
                                ackRequest.sendAckData(isaction);
                                socketIOClient.joinRoom(serviceGroup.getGroupName(groupID));
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    int adminID = serviceGroup.getAdminID(groupID);
                    for (Model_Client d : listClient) {
                        if (d.getUser().getUserID() == adminID) {
                            found = true;
                        }

                    }
                    if (found) {
                        for (Model_Client d : listClient) {
                            if (socketIOClient == d.getClient()) {
                                isaction = serviceGroup.checkMemberGroup(groupID, d.getUser().getUserID());
                                ackRequest.sendAckData(isaction);
                                socketIOClient.joinRoom(serviceGroup.getGroupName(groupID));
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    ackRequest.sendAckData(isaction);
                }

            }
        });
        server.addEventListener("get_member_group", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient socketIOClient, Integer groupID, AckRequest ackRequest) throws Exception {
                List<Integer> list = new ArrayList<>();
                list = serviceGroup.getMemberGroup(groupID);
                List<Model_User_Account> listU = new ArrayList<>();
                for (int l : list) {
                    listU.add(serviceUser.getOneUser(l));
                }
                ackRequest.sendAckData(listU.toArray());
            }
        });

//        server.addEventListener("join_group", String.class, new DataListener<String>() {
//            @Override
//            public void onData(SocketIOClient sioc, String t, AckRequest ar) throws Exception {
//                try {
//                    sioc.joinRoom(t);
//
//                    if (sioc.getAllRooms().isEmpty()) {
//                        System.out.println("Không tồn tại room");
//                    } else {
//                        System.out.println(sioc.getAllRooms().size());
//                    }
//                    sioc.sendEvent("join_group", t);
//                } catch (Exception e) {
//                    System.out.println(e + " o join group");
//                }
//            }
//        });
        //leave_group
        server.addEventListener("send_to_user", Model_Send_Message.class, new DataListener<Model_Send_Message>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Send_Message t, AckRequest ar) throws Exception {
                sendToClient(t, ar);
            }
        });

        server.addEventListener("send_to_group", Model_Send_Message.class, new DataListener<Model_Send_Message>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Send_Message t, AckRequest ar) throws Exception {
                String groupName = serviceGroup.getGroupName(t.getToUserID());
                sendToGroup(sioc, t, groupName, ar);
            }
        });

        server.addEventListener("send_avata_to_server", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient sioc, String t, AckRequest ar) throws Exception {
                System.out.println(t);
                String fileName = "";
                String fileExtension = "";
                int dotIndex = t.lastIndexOf(".");
                if (dotIndex != -1) {
                    fileName = t.substring(0, dotIndex);
                    fileExtension = t.substring(dotIndex, t.length());
                }
                Model_File file = serviceFile.addFileReceiver(fileName, fileExtension);
                serviceFile.initAvata(file);
                ar.sendAckData(file.getFileID());
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
                        if (message.getMessage().getMessageType() == MessageType.IMAGE.getValue()) {
                            Model_Receive_Image dataImage = new Model_Receive_Image();
                            dataImage.setFileID(t.getFileID());
                            Model_Send_Message messages = serviceFile.closeFileImage(dataImage);

                            sendTempFileImageToClient(messages, dataImage);
                        } else if (message.getMessage().getMessageType() == MessageType.FILE.getValue()) {
                            Model_Receive_File dataFile = new Model_Receive_File();
                            dataFile.setFileID(t.getFileID());
                            Model_Send_Message messages = serviceFile.closeFile(dataFile);

                            sendTempFileToClient(messages, dataFile);
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

        //send_avata
        server.addEventListener("send_avata", Model_Package_Sender.class, new DataListener<Model_Package_Sender>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Package_Sender data, AckRequest ackRequest) throws SQLException {
                try {
                    serviceFile.receiveAvata(data);
                    if (data.isFinish()) {
                        Model_File files = serviceFile.initFile(data.getFileID());
                        String filepath = PATH_FILE + files.getFileID() + "@" + files.getFileName() + files.getFileExtension();
                        File imageFile = new File(filepath);
                        int userID;
                        for (Model_Client d : listClient) {
                            if (d.getClient() == sioc) {
                                userID = d.getUser().getUserID();
                                serviceUser.setUserAvata(userID, imageFile);
                                updateAvata(sioc, userID);
                            }
                        }
                    } else {
                        ackRequest.sendAckData(true);
                    }
                } catch (IOException e) {
                    ackRequest.sendAckData(false);
                    e.printStackTrace();
                }
            }
        });

        //get_avata_user
        server.addEventListener("get_avata_user", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient client, Integer t, AckRequest ackRequest) throws SQLException {
                byte[] dataAvata = serviceUser.getUserAvata(t);
                ackRequest.sendAckData(dataAvata);
            }
        });

        server.addEventListener("send_file_group", Model_Package_Sender.class, new DataListener<Model_Package_Sender>() {
            @Override
            public void onData(SocketIOClient sioc, Model_Package_Sender t, AckRequest ar) throws Exception {
                try {
                    serviceFile.receiveFile(t);
                    if (t.isFinish()) {
                        ar.sendAckData(true);
                        Model_File_Receiver message = serviceFile.setColseFile(t.getFileID());
                        if (message.getMessage().getMessageType() == MessageType.IMAGE.getValue()) {
                            Model_Receive_Image dataImage = new Model_Receive_Image();
                            dataImage.setFileID(t.getFileID());
                            Model_Send_Message messages = serviceFile.closeFileImage(dataImage);
                            String groupName = serviceGroup.getGroupName(messages.getToUserID());

                            Model_File file = serviceFile.initFile(t.getFileID());
                            long fileSize = serviceFile.getFileSize(t.getFileID());
                            Model_Receive_Image_Group dataImageGroup = new Model_Receive_Image_Group(dataImage.getFileID(), file.getFileName(), file.getFileExtension(), fileSize, dataImage.getImage(), dataImage.getWidth(), dataImage.getHeight());

                            String filepath = PATH_FILE + file.getFileID() + "@" + file.getFileName() + file.getFileExtension();
                            Path path = Paths.get(filepath);
                            try {
                                byte[] dataIMM = Files.readAllBytes(path);
                                sendTempFileImageToGroup(sioc, messages, groupName, dataImageGroup, dataIMM);
                            } catch (IOException e) {
                                System.err.println("Lỗi khi đọc dữ liệu từ tệp tin: " + e.getMessage());
                            }

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
                ar.sendAckData(file.getFileName(), file.getFileExtension(), fileSize);
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

        server.addEventListener("list_data_user", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient sioc, String userID, AckRequest ar) throws Exception {
                try {
                    String[] parts = userID.split("@");
                    if (parts.length == 2) {
                        int toID = Integer.parseInt(parts[0]);
                        int uID = Integer.parseInt(parts[1]);
                        System.out.println("u1 = " + toID);
                        System.out.println("u2 = " + uID);
                        List<Model_Load_Data> list = serviceDataSave.getData(toID, uID);
                        sioc.sendEvent("list_data_user", list.toArray());
                    } else {
                        System.out.println("Invalid input format");
                    }
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }

        });
        //user_logout
        server.addEventListener("user_logout", Void.class, new DataListener<Void>() {
            @Override
            public void onData(SocketIOClient sioc, Void data, AckRequest ar) throws Exception {
                int userID = removeClient(sioc);
                if (userID != 0) {
                    //removed
                    userDisconnect(userID);
                    ar.sendAckData(true);
                } else {
                    ar.sendAckData(false);
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

    private void updateAvata(SocketIOClient sioc, int userID) {
        Collection<SocketIOClient> clients = server.getAllClients();
        for (SocketIOClient client : clients) {
            if (!client.equals(sioc)) {
                client.sendEvent("user_update_avata", userID);
            }
        }
    }

    private void groupAdd(Model_Group group) {
        System.out.println("gui thanh cong");
        server.getBroadcastOperations().sendEvent("group_status", group, true);
    }

    private void addClient(SocketIOClient client, Model_User_Account user) {
        listClient.add(new Model_Client(client, user));
    }

    private void sendToClient(Model_Send_Message data, AckRequest ar) throws SQLException {
        if (data.getMessageType() == MessageType.IMAGE.getValue() || data.getMessageType() == MessageType.FILE.getValue()) {
            try {
                Model_File file = serviceFile.addFileReceiver(data.getFileName(), data.getFileExtension());
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
                    c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), null, null));
                    break;
                }
            }
        }
    }

    private void sendToGroup(SocketIOClient sioc, Model_Send_Message data, String groupName, AckRequest ar) throws SQLException {
        if (data.getMessageType() == MessageType.IMAGE.getValue() || data.getMessageType() == MessageType.FILE.getValue()) {
            try {
                Model_File file = serviceFile.addFileReceiver(data.getFileName(), data.getFileExtension());
                serviceFile.initFile(file, data);
                ar.sendAckData(file.getFileID());
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        } else {
            Collection<SocketIOClient> clientCollection = server.getRoomOperations(groupName).getClients();
            Set<SocketIOClient> clients = new HashSet<>(clientCollection);
            System.out.println("size clients: " + clients.size());
            for (var client : clients) {
                if (!client.equals(sioc)) {
                    client.sendEvent("receive_ms_group", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), null, null), data.getToUserID());
                }
            }
        }
    }

    private void sendTempFileImageToClient(Model_Send_Message data, Model_Receive_Image dataImage) {
        for (Model_Client c : listClient) {
            if (c.getUser().getUserID() == data.getToUserID()) {
                //System.out.println("ID : " + dataImage.getFileID() + "he: " + dataImage.getHeight() + "wi: " + dataImage.getWidth());
                c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), dataImage, null));
                break;
            }
        }
    }

    private void sendTempFileToClient(Model_Send_Message data, Model_Receive_File dataFile) {
        for (Model_Client c : listClient) {
            if (c.getUser().getUserID() == data.getToUserID()) {
                //System.out.println("ID : " + dataFile.getFileID() + "he: " + dataFile.getFileExtension() + "wi: " + dataFile.getFileSize());
                c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText(), null, dataFile));
                break;
            }
        }
    }

    private void sendTempFileImageToGroup(SocketIOClient sioc, Model_Send_Message data, String groupName, Model_Receive_Image_Group dataImage, byte[] dataIm) {
        System.out.println("da gui anh ");
        //System.out.println("send_file_group dataIMM 3:" + Arrays.toString(dataIm));
        Collection<SocketIOClient> clientCollection = server.getRoomOperations(groupName).getClients();
        Set<SocketIOClient> clients = new HashSet<>(clientCollection);

        System.out.println("size clients: " + clients.size());
        for (var client : clients) {
            if (!client.equals(sioc)) {
                client.sendEvent("receive_image_group", new Model_Receive_Message_Group(data.getMessageType(), data.getFromUserID(), data.getToUserID(), data.getText(), dataImage, null), dataIm);
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
