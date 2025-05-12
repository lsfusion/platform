package lsfusion.http.authentication;

import lsfusion.interop.connection.AuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public class LSFAuthTokenFilter extends OncePerRequestFilter {

    // maybe later it will make sense to extend AbstractAuthenticationProcessingFilter as sometimes is recommended(with overriding continueChainBeforeSuccessfulAuthentication and empty AuthenticationSuccessHandler to suppress redirect)

    private final AuthenticationManager authenticationManager;

    public LSFAuthTokenFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticationToken token = new AuthenticationToken(header.substring(7));
        if (!token.isAnonymous() && !token.string.contains(".")) {
            logger.error("Generated jwt token without dot: " + token);
        }
        LSFAuthenticationToken auth = new LSFAuthenticationToken("", "", token, Locale.getDefault());
        Authentication authResult = authenticationManager.authenticate(auth);

        SecurityContextHolder.getContext().setAuthentication(authResult);

        chain.doFilter(request, response);
    }
}
