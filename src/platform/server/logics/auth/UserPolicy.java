package platform.server.logics.auth;

import java.util.List;
import java.util.ArrayList;

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
