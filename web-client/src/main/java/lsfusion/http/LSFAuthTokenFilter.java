package lsfusion.http;

import lsfusion.interop.remote.AuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public class LSFAuthTokenFilter extends OncePerRequestFilter {

    // maybe later it will make sense to extend AbstractAuthenticationProcessingFilter as sometimes is recommended(with overriding continueChainBeforeSuccessfulAuthentication and empty AuthenticationSuccessHandler to suppress redirect)

    @Autowired
    private AuthenticationManager authenticationManager;

    public LSFAuthTokenFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        LSFAuthenticationToken auth = new LSFAuthenticationToken("", "", new AuthenticationToken(header.substring(7)), Locale.getDefault());
        Authentication authResult = authenticationManager.authenticate(auth);

        SecurityContextHolder.getContext().setAuthentication(authResult);

        chain.doFilter(request, response);
    }
}
