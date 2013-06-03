package lsfusion.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class PropertyReadAccessFilter extends GenericFilterBean {
    @Autowired
    private BusinessLogicsProvider blProvider;

    private String URI;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String requestUri = ((HttpServletRequest) servletRequest).getServletPath();

        if (requestUri.equals(URI)) {
            boolean defaultPermission = blProvider.getLogics().checkDefaultViewPermission(servletRequest.getParameter("sid"));

            if (!defaultPermission && SecurityContextHolder.getContext().getAuthentication() == null) {
                throw new AccessDeniedException("Access denied for non-authenticated user");
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void setURI(String uri) {
        URI = uri;
    }
}
