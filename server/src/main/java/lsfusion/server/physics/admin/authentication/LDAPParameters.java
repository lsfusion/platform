package lsfusion.server.physics.admin.authentication;

import java.util.List;
import java.util.Map;

public class LDAPParameters {

    private final boolean connected;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final List<String> groupNames;
    private final Map<String, String> attributes;

    public LDAPParameters(boolean connected, String firstName, String lastName, String email, List<String> groupNames, Map<String, String> attributes) {
        this.connected = connected;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.groupNames = groupNames;
        this.attributes = attributes;
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

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
