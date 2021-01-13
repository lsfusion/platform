package lsfusion.http.authentication;

import lsfusion.base.ServerUtils;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.ServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LSFAuthAnonymousFilter extends OncePerRequestFilter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private LogicsProvider logicsProvider;

    public LSFAuthAnonymousFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        Authentication existingAuth = SecurityContextHolder.getContext()
                .getAuthentication();

        // if there is no authentication and server supports anonymous UI, give "anonymous authentication"
        ServerSettings serverSettings;
        if ((existingAuth == null || !existingAuth.isAuthenticated() || existingAuth instanceof AnonymousAuthenticationToken) && (serverSettings = logicsProvider.getServerSettings(request, false)) != null && serverSettings.anonymousUI) {
            LSFAuthenticationToken auth = new LSFAuthenticationToken("", "", AuthenticationToken.ANONYMOUS, ServerUtils.getLocale(request));
            Authentication authResult = authenticationManager.authenticate(auth);

            SecurityContextHolder.getContext().setAuthentication(authResult);
        }

        chain.doFilter(request, response);
    }
}
