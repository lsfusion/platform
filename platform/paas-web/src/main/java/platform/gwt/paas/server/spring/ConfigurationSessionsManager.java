package platform.gwt.paas.server.spring;

public interface ConfigurationSessionsManager {
    public void remove(ConfigurationSessionWrapper session);

    ConfigurationSessionWrapper getWrappedSession(String innerId, boolean create);
}
