package platform.server.logics.auth;

import platform.interop.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class User extends UserPolicy {

    String login;
    String password;

    public UserInfo userInfo;

    public User(String ilogin, String ipassword, UserInfo iuserInfo) {

        login = ilogin;
        password = ipassword;
        userInfo = iuserInfo;
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
