package platformlocal;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.Serializable;

public class AuthPolicy {

    Map<String, User> users = new HashMap();

    User addUser(String login, String password) {
        return addUser(login, password, new UserInfo());
    }

    User addUser(String login, String password, UserInfo userInfo) {

        User user = new User(login, password, userInfo);
        users.put(login, user);
        return user;
    }

    User getUser(String login, String password) {

        User user = users.get(login);
        if (user == null) return null;

        if (!password.equals(user.password)) return null;

        return user;
    }

    SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();

    public SecurityPolicy getSecurityPolicy(User user) {

        SecurityPolicy securityPolicy = new SecurityPolicy();
        securityPolicy.override(defaultSecurityPolicy);
        securityPolicy.override(user.getSecurityPolicy());

        return securityPolicy;
    }
}

class UserPolicy {

    List<SecurityPolicy> securityPolicies = new ArrayList();

    public void addSecurityPolicy(SecurityPolicy policy) {
        securityPolicies.add(policy);
    }

    public SecurityPolicy getSecurityPolicy() {
        
        SecurityPolicy resultPolicy = new SecurityPolicy();
        for (SecurityPolicy policy : securityPolicies) {
            resultPolicy.override(policy);
        }

        return resultPolicy;
    }
}

class User extends UserPolicy {

    String login;
    String password;

    UserInfo userInfo;

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

class UserInfo implements Serializable {

    String firstName = "";
    String lastName = "";

    UserInfo() {

    }

    UserInfo(String ifirstName, String ilastName) {

        firstName = ifirstName;
        lastName = ilastName;
    }

}

class UserGroup extends UserPolicy {

}

class SecurityPolicy {

    public void override(SecurityPolicy policy) {

    }
}
