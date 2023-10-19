
package com.app.apps;

public enum GroupMemRole {
    ADMIN(1), MEMBER(2);
    private final int value;

    public int getValue() {
        return value;
    }

    private GroupMemRole(int value) {
        this.value = value;
    }
    
    public static GroupMemRole toRoleType(int value){
        if(value == 1){
            return ADMIN;
        } else 
            return MEMBER;
    }
}
