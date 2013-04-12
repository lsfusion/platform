package platform.gwt.paas.server.spring.filters;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import platform.gwt.paas.server.spring.ConfigurationsUrlHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfigurationAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String url = ConfigurationsUrlHelper.getLoginUrl(request);
        redirectStrategy.sendRedirect(request, response, url);
    }
}
