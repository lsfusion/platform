package platform.gwt.paas.server.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class ConfigurationSessionAwareRequest extends HttpServletRequestWrapper {
    private final String innerSessionId;

    public ConfigurationSessionAwareRequest(HttpServletRequest request, String innerSessionId) {
        super(request);
        this.innerSessionId = innerSessionId;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        return ConfigurationSessionsManagerImpl.getWrappedSession((HttpServletRequest) getRequest(), innerSessionId, create);
    }
}
