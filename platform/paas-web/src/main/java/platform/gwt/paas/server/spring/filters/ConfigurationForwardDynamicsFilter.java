package platform.gwt.paas.server.spring.filters;

import org.springframework.web.filter.GenericFilterBean;
import platform.gwt.paas.server.spring.ConfigurationsUrlHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ConfigurationForwardDynamicsFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String relativeUrl = ConfigurationsUrlHelper.getConfigurationRelativeDynamicEnd(httpRequest);
        if (relativeUrl != null) {
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("/form/" + relativeUrl);
            requestDispatcher.forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
