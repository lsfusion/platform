package platform.gwt.paas.server.spring.filters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;
import platform.gwt.paas.server.spring.ConfigurationBLProvider;
import platform.gwt.paas.server.spring.ConfigurationsUrlHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ConfigurationBLSetupFilter extends GenericFilterBean {
    @Autowired
    private ConfigurationBLProvider blProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        int configurationId = ConfigurationsUrlHelper.getConfigurationIdFromRequest((HttpServletRequest) request);
        if (configurationId != -1) {
            blProvider.initCurrentProvider(configurationId);
        } else {
            blProvider.setCurrentProviderToPaas();
        }

        chain.doFilter(request, response);
    }
}
