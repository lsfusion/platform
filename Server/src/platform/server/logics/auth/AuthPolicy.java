package platform.server.logics.auth;

import platform.interop.UserInfo;

import java.util.HashMap;
import java.util.Map;

public class AuthPolicy {

    Map<String, User> users = new HashMap();

    User addUser(String login, String password) {
        return addUser(login, password, new UserInfo());
    }

    public User addUser(String login, String password, UserInfo userInfo) {

        User user = new User(login, password, userInfo);
        users.put(login, user);
        return user;
    }

    public User getUser(String login, String password) {

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

