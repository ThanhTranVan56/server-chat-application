package com.app.model;

public class Model_Receive_Message {

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

    public Model_Receive_Image getDataImage() {
        return dataImage;
    }

    public void setDataImage(Model_Receive_Image dataImage) {
        this.dataImage = dataImage;
    }
    
    public Model_Receive_File getDataFile() {
        return dataFile;
    }

    public void setDataFile(Model_Receive_File dataFile) {
        this.dataFile = dataFile;
    }
    
    public Model_Receive_Message(int messageType, int fromUserID, String text, Model_Receive_Image dataImage, Model_Receive_File dataFile) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.text = text;
        this.dataImage = dataImage;
        this.dataFile = dataFile;
    }

    public Model_Receive_Message() {
    }

    private int messageType;
    private int fromUserID;
    private String text;
    private Model_Receive_Image dataImage;
    private Model_Receive_File dataFile;

    

    

}
