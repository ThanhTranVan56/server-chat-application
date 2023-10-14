package com.app.model;

public class Model_Receive_File {

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Model_Receive_File(int fileID, String fileName, long fileSize, String fileExtension, byte[] data) {
        this.fileID = fileID;
        this.fileSize = fileSize;
        this.fileExtension = fileExtension;
        this.data = data;
    }

    public Model_Receive_File() {
    }

    private int fileID;
    private long fileSize;
    private String fileExtension;
    private byte[] data;

}
