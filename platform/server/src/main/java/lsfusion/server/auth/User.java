package lsfusion.server.auth;

import java.util.ArrayList;
import java.util.List;

public class User extends PolicyAgent {

    public final int ID;

    public User(int ID) {

        this.ID = ID;
    }

    List<UserGroup> userGroups = new ArrayList();

    public SecurityPolicy getSecurityPolicy() {

        SecurityPolicy resultPolicy = new SecurityPolicy();

        for (UserGroup userGroup : userGroups)
            resultPolicy.override(userGroup.getSecurityPolicy());

        resultPolicy.override(super.getSecurityPolicy());
        return resultPolicy;
    }
}
