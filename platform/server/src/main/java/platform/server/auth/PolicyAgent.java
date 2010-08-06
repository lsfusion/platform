package platform.server.auth;

import java.util.ArrayList;
import java.util.List;

class PolicyAgent {

    List<SecurityPolicy> securityPolicies = new ArrayList<SecurityPolicy>();

    public void addSecurityPolicy(SecurityPolicy policy) {
        securityPolicies.add(policy);
    }

    public SecurityPolicy getSecurityPolicy() {

        SecurityPolicy resultPolicy = new SecurityPolicy(-1);
        for (SecurityPolicy policy : securityPolicies) {
            resultPolicy.override(policy);
        }

        return resultPolicy;
    }
}
