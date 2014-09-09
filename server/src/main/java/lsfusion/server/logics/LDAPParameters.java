package lsfusion.server.logics;

import java.util.List;

public class LDAPParameters {

    private boolean connected;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> groupNames;

    public LDAPParameters(boolean connected, String firstName, String lastName, String email, List<String> groupNames) {
        this.connected = connected;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.groupNames = groupNames;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
    
    public List<String> getGroupNames() {
        return groupNames;
    }
}                       
