package platform.server.auth;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class PolicyManager {

    //используем синхронизированный Map
    private Map<Integer, User> users = new Hashtable<Integer, User>();

    private Map<Integer, SecurityPolicy> policies = new HashMap<Integer, SecurityPolicy>();

    public static SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();

    public SecurityPolicy getSecurityPolicy(User user) {

        SecurityPolicy securityPolicy = new SecurityPolicy();
        securityPolicy.override(defaultSecurityPolicy);
        securityPolicy.override(user.getSecurityPolicy());

        return securityPolicy;
    }

    public void putPolicy(Integer policyID, SecurityPolicy policy) {
        policies.put(policyID, policy);
    }

    public void putUser(Integer userId, User user) {
        users.put(userId, user);
    }

    public SecurityPolicy getPolicy(Integer policyID) {
        return policies.get(policyID);
    }

    public User getUser(Integer userId) {
        return users.get(userId);
    }
}

