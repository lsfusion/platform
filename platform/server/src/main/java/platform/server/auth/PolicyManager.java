package platform.server.auth;

import platform.base.col.MapFact;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.add.MAddMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyManager {
    private MAddMap<Integer, SecurityPolicy> policies = MapFact.mAddOverrideMap();

    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();
    public SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();
    public MAddMap<Integer, List<SecurityPolicy>> userPolicies = MapFact.mAddOverrideMap();

    public void putPolicy(Integer policyID, SecurityPolicy policy) {
        policies.add(policyID, policy);
    }

    public SecurityPolicy getPolicy(Integer policyID) {
        return policies.get(policyID);
    }
}

