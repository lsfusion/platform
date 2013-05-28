package platform.gwt.paas.server.spring;

import org.springframework.security.web.savedrequest.Enumerator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationSessionWrapper implements HttpSession {
    private final static String[] EMPTY_ARRAY = new String[0];

    private final ConfigurationSessionsManager sessionManager;

    private final HttpSession realSession;

    private final String innerId;

    private final String sessionId;

    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    public ConfigurationSessionWrapper(ConfigurationSessionsManager sessionManager, HttpSession realSession, String innerId) {
        this.sessionManager = sessionManager;
        this.realSession = realSession;
        this.innerId = innerId;
        this.sessionId = realSession.getId() + "_" + innerId;
    }

    @Override
    public Object getAttribute(String name) {
        if (name == null) {
            return null;
        }
        return attributes.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration getAttributeNames() {
        return new Enumerator(attributes.keySet(), true);
    }

    @Override
    public String[] getValueNames() {
        return keys();
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("attribute name can't be null");
        }

        if (value == null) {
            removeAttribute(name);
        }

        attributes.put(name, value);
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        if (name == null) {
            return;
        }
        attributes.remove(name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    private String[] keys() {
        return attributes.keySet().toArray(EMPTY_ARRAY);
    }

    public String getInnerId() {
        return innerId;
    }

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public void invalidate() {
        attributes.clear();
        sessionManager.remove(this);
    }

    @Override
    public boolean isNew() {
        return realSession.isNew();
    }

    @Override
    public long getCreationTime() {
        return realSession.getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return realSession.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return realSession.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        realSession.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return realSession.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return realSession.getSessionContext();
    }
}
