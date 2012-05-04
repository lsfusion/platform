package platform.server.auth;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PolicyManager {
    private Map<Integer, SecurityPolicy> policies = new HashMap<Integer, SecurityPolicy>();

    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();
    public SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();
    public Map<Integer, List<SecurityPolicy>> userPolicies = new HashMap<Integer, List<SecurityPolicy>>();

    public void putPolicy(Integer policyID, SecurityPolicy policy) {
        policies.put(policyID, policy);
    }

    public SecurityPolicy getPolicy(Integer policyID) {
        return policies.get(policyID);
    }
}

