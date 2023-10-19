package com.app.model;

public class Model_Group {

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    
    public int getAdminID() {
        return adminID;
    }

    public void setAdminID(int adminID) {
        this.adminID = adminID;
    }
    public Model_Group(int groupID, int adminID, String groupName, String image) {
        this.groupID = groupID;
        this.adminID = adminID;
        this.groupName = groupName;
        this.image = image;
    }
    
    private int groupID;
    private int adminID;
    private String groupName;
    private String image;

}
