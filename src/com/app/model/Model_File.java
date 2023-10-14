package com.app.model;


public class Model_File {

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
    
    public Model_File(int fileID, String fileName, String fileExtension){
        this.fileID = fileID;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }
    public Model_File(){
    
    }
    private int fileID;
    private String fileName;
    private String fileExtension;

    
}
