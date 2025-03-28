package lsfusion.http.authentication;

import lsfusion.http.controller.ExternalRequestHandler;
import lsfusion.http.controller.MainController;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

public class LSFUrlAuthenticationFilter extends OncePerRequestFilter {
    private final LSFRemoteAuthenticationProvider authenticationProvider;

    public LSFUrlAuthenticationFilter(LSFRemoteAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userName = request.getParameter("user");
        String password = request.getParameter("password");

        if (authenticationProvider != null && userName != null && password != null) {
            try {
                SecurityContextHolder.getContext().setAuthentication(MainController.getAuthentication(request, userName, password, authenticationProvider));
                response.sendRedirect(MainController.getURLPreservingParameters(request.getRequestURI(), Arrays.asList("user", "password"), request));
            } catch (Exception e) {
                ExternalRequestHandler.processFailedAuthentication(request, response, e, Arrays.asList("user", "password"));
            }
            return;
        }
        filterChain.doFilter(request, response);
    }
}
