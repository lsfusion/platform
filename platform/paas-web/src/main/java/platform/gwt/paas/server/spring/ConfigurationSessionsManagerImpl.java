package platform.gwt.paas.server.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationSessionsManagerImpl implements ConfigurationSessionsManager {
    private final HttpSession session;

    private final Map<String, ConfigurationSessionWrapper> configurationSessions = Collections.synchronizedMap(new HashMap<String, ConfigurationSessionWrapper>());

    public ConfigurationSessionsManagerImpl(HttpSession session) {
        this.session = session;
    }

    @Override
    public ConfigurationSessionWrapper getWrappedSession(String innerId, boolean create) {
        ConfigurationSessionWrapper wrappedSession = configurationSessions.get(innerId);
        if (wrappedSession == null && create) {
            synchronized (configurationSessions) {
                wrappedSession = configurationSessions.get(innerId);
                if (wrappedSession == null) {
                    wrappedSession = new ConfigurationSessionWrapper(this, session, innerId);
                    configurationSessions.put(innerId, wrappedSession);
                }
            }
        }
        return wrappedSession;
    }

    @Override
    public void remove(ConfigurationSessionWrapper session) {
        configurationSessions.remove(session.getInnerId());
    }


    private static final String CONFIG_SESSION_MANAGER_KEY = "__config.session.manager.key";
    public static ConfigurationSessionsManagerImpl getInstance(final HttpSession realSession) {
        if (realSession == null) {
            throw new IllegalStateException("Session must not be null!");
        }
        synchronized (realSession) {
            ConfigurationSessionsManagerImpl currentManager = (ConfigurationSessionsManagerImpl) realSession.getAttribute(CONFIG_SESSION_MANAGER_KEY);
            if (currentManager == null) {
                currentManager = new ConfigurationSessionsManagerImpl(realSession);
                realSession.setAttribute(CONFIG_SESSION_MANAGER_KEY, currentManager);
            }
            return currentManager;
        }
    }

    public static ConfigurationSessionWrapper getWrappedSession(HttpServletRequest request, String innerId, boolean create) {
        HttpSession realSession = request.getSession(create);
        return realSession == null ? null : getInstance(realSession).getWrappedSession(innerId, create);
    }
}
