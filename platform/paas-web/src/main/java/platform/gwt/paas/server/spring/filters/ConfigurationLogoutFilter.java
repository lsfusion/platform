package platform.gwt.paas.server.spring.filters;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.filter.GenericFilterBean;
import platform.gwt.paas.server.spring.ConfigurationsUrlHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class ConfigurationLogoutFilter extends GenericFilterBean {

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    private String filterProcessingUrl = "/logout";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        if (uri.endsWith(filterProcessingUrl)) {

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            SecurityContextHolder.clearContext();

            String url = ConfigurationsUrlHelper.getSuccessfullLogoutUrl(request);
            redirectStrategy.sendRedirect(request, response, url);
        } else {
            chain.doFilter(request, response);
        }
    }
}
