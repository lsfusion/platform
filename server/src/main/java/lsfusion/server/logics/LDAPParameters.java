package lsfusion.server.logics;

public class LDAPParameters {

    private boolean connected;
    private String groupName;
    private String firstName;
    private String lastName;
    private String email;

    public LDAPParameters(boolean connected, String firstName, String lastName, String email) {
        this.connected = connected;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public LDAPParameters(boolean connected, String firstName, String lastName, String email, String groupName) {
        this(connected, firstName, lastName, email);
        this.groupName = groupName;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getGroupName() {
        return groupName;
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
}
