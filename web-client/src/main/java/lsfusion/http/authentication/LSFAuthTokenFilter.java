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

    // the token of an "Authorization: Bearer" header, if the request has one
    public static String getBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.toLowerCase().startsWith("bearer ") ? header.substring(7) : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = getBearerToken(request);

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        LSFAuthenticationToken auth = new LSFAuthenticationToken("", "", new AuthenticationToken(token), Locale.getDefault());
        Authentication authResult = authenticationManager.authenticate(auth);

        SecurityContextHolder.getContext().setAuthentication(authResult);

        chain.doFilter(request, response);
    }
}
