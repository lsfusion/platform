package lsfusion.http.authentication;

import lsfusion.http.controller.MainController;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private LSFRemoteAuthenticationProvider authenticationProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userName = request.getParameter("user");
        String password = request.getParameter("password");

        if (userName != null && password != null) {
            String redirectUrl;
            try {
                SecurityContextHolder.getContext().setAuthentication(MainController.getAuthentication(request, userName, password, authenticationProvider));
                redirectUrl = request.getRequestURI();
            } catch (Exception e) {
                request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
                redirectUrl = "/login";
            }
            response.sendRedirect(MainController.getURLPreservingParameters(redirectUrl, Arrays.asList("user", "password"), request));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
