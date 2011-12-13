package platform.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PropertyReadAccessFilter extends GenericFilterBean {
    @Autowired
    private BusinessLogicsProvider blProvider;

    private String URI;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String requestUri = ((HttpServletRequest) servletRequest).getRequestURI();

        if (requestUri.equals(URI)) {
            boolean defaultPermission = blProvider.getLogics().checkDefaultViewPermission(servletRequest.getParameter("sid"));

            HttpRequestResponseHolder holder = new HttpRequestResponseHolder((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
            SecurityContext contextBeforeChainExecution = new HttpSessionSecurityContextRepository().loadContext(holder);

            if (!defaultPermission && contextBeforeChainExecution.getAuthentication() == null) {
                throw new AuthenticationException("Access denied for non-authenticated user"){};
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void setURI(String uri) {
        URI = uri;
    }
}
