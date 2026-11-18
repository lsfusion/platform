package lsfusion.http.authentication;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// The web client's cookies of the browser's session. A response sets the site cookie (LSFCrossSiteRequestFilter)
// unless its request has it.
public class LSFSessionCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        LSFCrossSiteRequestFilter.setSiteCookie(request, response);
        chain.doFilter(request, response);
    }
}
