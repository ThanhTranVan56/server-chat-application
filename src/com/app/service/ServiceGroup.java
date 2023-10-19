package com.app.service;

import com.app.connection.DatabaseConnection;
import com.app.model.Model_Group;
import com.app.model.Model_Message;
import com.app.model.Model_Register_Group;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceGroup {

    public ServiceGroup() {
        this.con = DatabaseConnection.getInstance().getConnection();
    }

    public Model_Message registerGroup(Model_Register_Group data) {
        //Check group exit
        Model_Message message = new Model_Message();
        try {
            PreparedStatement p = con.prepareStatement(CHECK_GROUP);
            p.setString(1, data.getGroupName());
            ResultSet r = p.executeQuery();
            if (r.next()) {
                message.setAction(false);
                message.setMessage("Group Already Exit");

            } else {
                message.setAction(true);
            }
            r.close();
            p.close();
            if (message.isAction()) {
                //Insert group Register
                con.setAutoCommit(false);
                p = con.prepareStatement(INSERT_GROUP, PreparedStatement.RETURN_GENERATED_KEYS);
                p.setString(1, data.getGroupName());
                p.setInt(2, data.getAdminID());
                p.execute();
                r = p.getGeneratedKeys();
                r.next();
                int groupID = r.getInt(1);
                r.close();
                p.close();
                //creat group member
                p = con.prepareStatement(INSERT_GROUP_MEMBER);
                p.setInt(1, groupID);
                p.setInt(2, data.getAdminID());
                p.setString(3, "1");
                p.setString(4, "1");
                p.execute();
                p.close();
                con.commit();
                con.setAutoCommit(true);
                message.setAction(true);
                message.setMessage("OK");
                message.setData(new Model_Group(groupID, data.getAdminID(), data.getGroupName(), ""));
            }

        } catch (SQLException e) {
            message.setAction(false);
            message.setMessage("Server Error");
            try {
                if (con.getAutoCommit() == false) {
                    con.rollback();
                    con.setAutoCommit(true);
                }
            } catch (SQLException e1) {

            }
        }
        return message;
    }

    public String getGroupName(int groupID) throws SQLException {
        PreparedStatement p = con.prepareStatement(SELECT_GROUP_NAME);
        p.setInt(1, groupID);
        ResultSet r = p.executeQuery();
        r.next();
        String groupName = r.getString(1);
        p.close();
        r.close();
        return groupName;
    }

    public List<Model_Group> getGroups() throws SQLException {
        List<Model_Group> list = new ArrayList<>();
        PreparedStatement p = con.prepareStatement(SELECT_ALL_GROUP);
        ResultSet r = p.executeQuery();
        while (r.next()) {
            int groupID = r.getInt(1);
            int adminID = r.getInt(2);
            String groupName = r.getString(3);
            String image = r.getString(4);
            list.add(new Model_Group(groupID, adminID, groupName, image));
        }
        r.close();
        p.close();
        return list;
    }

    public Model_Group getGroup(String groupName) throws SQLException {
        Model_Group group;
        PreparedStatement p = con.prepareStatement(SELECT_GROUP);
        p.setString(1, groupName);
        ResultSet r = p.executeQuery();
        r.next();
        int groupID = r.getInt(1);
        int adminID = r.getInt(2);
        group = new Model_Group(groupID, adminID, groupName, null);
        r.close();
        p.close();
        return group;
    }

    public int getAdmin(String groupName) throws SQLException {
        PreparedStatement p = con.prepareStatement(SELECT_GROUP_ADMIN);
        p.setString(1, groupName);
        ResultSet r = p.executeQuery();
        r.next();
        int adminID = r.getInt(1);
        r.close();
        p.close();
        return adminID;
    }

    public int getGroupID(String groupName) throws SQLException {
        PreparedStatement p = con.prepareStatement(SELECT_GROUPID);
        p.setString(1, groupName);
        ResultSet r = p.executeQuery();
        r.next();
        int groupID = r.getInt(1);
        r.close();
        p.close();
        return groupID;
    }

    public void setMemberGroup(int groupID, int memberID) throws SQLException {
        try {
            PreparedStatement p = con.prepareStatement(INSERT_GROUP_MEMBER);
            p.setInt(1, groupID);
            p.setInt(2, memberID);
            p.setString(3, "0");
            p.setString(4, "1");
            p.execute();
            p.close();
        } catch (SQLException e) {
            System.out.println("loi o day "+ e);
        }
    }

    public boolean checkMemberGroup(int groupID, int memberID) throws SQLException {
        boolean action = false;
        PreparedStatement p = con.prepareStatement(CHECK_MEMBER);
        p.setInt(1, groupID);
        ResultSet r = p.executeQuery();
        while (r.next()) {
            int memID = r.getInt(1);
            if (memID == memberID) {
                action = true;
            }
        }
        r.close();
        p.close();
        return action;
    }
    
    public List<Integer> getMemberGroup(int groupID) throws SQLException {
        List<Integer> listID = new ArrayList<>();
        PreparedStatement p = con.prepareStatement(CHECK_MEMBER);
        p.setInt(1, groupID);
        ResultSet r = p.executeQuery();
        while (r.next()) {
            int memID = r.getInt(1);
            listID.add(memID);
        }
        r.close();
        p.close();
        return listID;
    }
    //SQL
    private final String CHECK_GROUP = "select AdminID from `group` where GroupName = ?";
    private final String INSERT_GROUP = "INSERT INTO `group` (GroupName, AdminID) VALUES (?, ?)";
    private final String INSERT_GROUP_MEMBER = "insert into group_member (GroupID, MemberID, MemberRole, MemberStatus) values (?,?,?,?)";
    private final String SELECT_ALL_GROUP = "select GroupID, AdminID, GroupName, Image from `group`";
    private final String SELECT_GROUP = "select GroupID, AdminID, Image from `group` where GroupName = ?";
    private final String SELECT_GROUP_NAME = "select GroupName from `group` where GroupID = ?";
    private final String SELECT_GROUP_ADMIN = "select AdminID from `group` where GroupName = ?";
    private final String SELECT_GROUPID = "select GroupID from `group` where GroupName = ?";
    private final String CHECK_MEMBER = "select MemberID from `group_member` where GroupID = ?";
    //Instance
    private final Connection con;
}
