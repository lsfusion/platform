package platform.gwt.paas.server.spring;

import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;

public interface ConfigurationBLProvider extends BusinessLogicsProvider<RemoteLogicsInterface> {
    public void initCurrentProvider(int configurationId) throws IOException;

    public void setCurrentProviderToPaas();

    public BusinessLogicsProvider getCurrentProvider();
}
