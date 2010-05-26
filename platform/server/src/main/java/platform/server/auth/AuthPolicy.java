package platform.server.auth;

import platform.interop.UserInfo;

import java.util.HashMap;
import java.util.Map;

public class AuthPolicy {

    public Map<Integer, User> users = new HashMap<Integer, User>();

    SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();

    public SecurityPolicy getSecurityPolicy(User user) {

        SecurityPolicy securityPolicy = new SecurityPolicy();
        securityPolicy.override(defaultSecurityPolicy);
        securityPolicy.override(user.getSecurityPolicy());

        return securityPolicy;
    }
}

