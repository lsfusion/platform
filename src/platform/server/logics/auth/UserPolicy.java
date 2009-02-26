package platform.server.logics.auth;

import java.util.ArrayList;
import java.util.List;

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
