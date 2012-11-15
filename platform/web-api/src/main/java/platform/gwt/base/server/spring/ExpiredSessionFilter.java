package platform.gwt.base.server.spring;

import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class ExpiredSessionFilter extends GenericFilterBean {
    private static final String FILTER_APPLIED = "__spring_security_expired_session_filter_applied";

    private Pattern urlPattern;

    @Override
     public void afterPropertiesSet() {
         Assert.notNull(urlPattern, "urlPattern must be specified");
     }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (request.getAttribute(FILTER_APPLIED) != null) {
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

        String requestUrl = UrlUtils.buildRequestUrl(request);

        if (request.getRequestedSessionId() != null
            && !request.isRequestedSessionIdValid()
            && urlPattern.matcher(requestUrl).matches()) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SESSION_TIMED_OUT");
            return;
        }

        chain.doFilter(request, response);
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = Pattern.compile(urlPattern);
    }
}
