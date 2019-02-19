package lsfusion.http;

import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.remote.AuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

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
        if ((existingAuth == null || !existingAuth.isAuthenticated() || existingAuth instanceof AnonymousAuthenticationToken) && logicsProvider.getServerSettings(request).anonymousUI) {
            LSFAuthenticationToken auth = new LSFAuthenticationToken("", "", AuthenticationToken.ANONYMOUS, Locale.getDefault());
            Authentication authResult = authenticationManager.authenticate(auth);

            SecurityContextHolder.getContext().setAuthentication(authResult);
        }

        chain.doFilter(request, response);
    }
}
