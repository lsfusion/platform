package platform.gwt.base.server;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class ServerUtils {
    public static Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication auth = securityContext.getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Not authorized");
//            auth = new TestingAuthenticationToken("admin", "test");
        }
        return auth;
    }
}
