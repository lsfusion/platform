package platform.gwt.paas.server.spring.filters;

import org.springframework.web.filter.GenericFilterBean;
import platform.gwt.paas.server.spring.ConfigurationSessionAwareRequest;
import platform.gwt.paas.server.spring.ConfigurationsUrlHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ConfigurationSessionSetupFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        int configurationId = ConfigurationsUrlHelper.getConfigurationIdFromRequest((HttpServletRequest) request);
        if (configurationId != -1) {
            request = new ConfigurationSessionAwareRequest(httpRequest, "configuration" + configurationId);
            request.setAttribute(ConfigurationsUrlHelper.CONFIGURATION_ID_KEY, configurationId);
        } else {
            request.removeAttribute(ConfigurationsUrlHelper.CONFIGURATION_ID_KEY);
            request = new ConfigurationSessionAwareRequest(httpRequest, "paas");
        }
        chain.doFilter(request, response);
    }
}
