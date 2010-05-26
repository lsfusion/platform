package platform.server.auth;

import platform.interop.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class User extends UserPolicy {

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
