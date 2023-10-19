package com.app.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Model_Avata_Receiver {

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public RandomAccessFile getAccFile() {
        return accFile;
    }

    public void setAccFile(RandomAccessFile accFile) {
        this.accFile = accFile;
    }

    public Model_Avata_Receiver(File file) throws IOException{
        this.file = file;
        this.accFile = new RandomAccessFile(file, "rw");
    }

    public Model_Avata_Receiver() {
    }
    private File file;
    private RandomAccessFile accFile;
    
    public synchronized long writeFile(byte[] data) throws IOException{
        accFile.seek(accFile.length());
        accFile.write(data);
        return accFile.length();
    }
    
    public void close() throws IOException{
        accFile.close();
    }
}
