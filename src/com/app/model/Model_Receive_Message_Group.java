package com.app.model;

public class Model_Receive_Message_Group {

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getFromUserID() {
        return fromUserID;
    }

    public void setFromUserID(int fromUserID) {
        this.fromUserID = fromUserID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Model_Receive_Image_Group getDataImage() {
        return dataImage;
    }

    public void setDataImage(Model_Receive_Image_Group dataImage) {
        this.dataImage = dataImage;
    }
    
    public Model_Receive_File getDataFile() {
        return dataFile;
    }

    public void setDataFile(Model_Receive_File dataFile) {
        this.dataFile = dataFile;
    }
    
    public int getToGroupID() {
        return toGroupID;
    }

    public void setToGroupID(int toGroupID) {
        this.toGroupID = toGroupID;
    }
    
    public Model_Receive_Message_Group(int messageType, int fromUserID,int toGroupID , String text, Model_Receive_Image_Group dataImage, Model_Receive_File dataFile) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.toGroupID = toGroupID;
        this.text = text;
        this.dataImage = dataImage;
        this.dataFile = dataFile;
    }

    public Model_Receive_Message_Group() {
    }

    private int messageType;
    private int fromUserID;
    private int toGroupID;
    private String text;
    private Model_Receive_Image_Group dataImage;
    private Model_Receive_File dataFile;


}
