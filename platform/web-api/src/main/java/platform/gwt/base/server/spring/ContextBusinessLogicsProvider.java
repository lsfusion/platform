package platform.gwt.base.server.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import platform.interop.RemoteLogicsInterface;

import javax.servlet.ServletContext;

import static platform.base.BaseUtils.nvl;

public class ContextBusinessLogicsProvider<T extends RemoteLogicsInterface> extends SingleBusinessLogicsProvider<T> implements InitializingBean {

    @Autowired
    private ServletContext servletContext;

    private String registryHostKey = "registryHost";
    private String registryPortKey = "registryPort";
    private String exportNameKey = "exportName";

    @Override
    public void afterPropertiesSet() throws Exception {
        String registryHost = servletContext.getInitParameter(registryHostKey);
        String registryPort = servletContext.getInitParameter(registryPortKey);
        String exportName = nvl(servletContext.getInitParameter(exportNameKey), "default");
        if (registryHost == null || registryPort == null) {
            throw new IllegalStateException(registryHostKey + " or " + registryPortKey + " parameters aren't set in web.xml");
        }

        setRegistryHost(registryHost);
        setRegistryPort(Integer.parseInt(registryPort));
        setExportName(exportName);
    }

    public void setRegistryHostKey(String registryHostKey) {
        this.registryHostKey = registryHostKey;
    }

    public void setRegistryPortKey(String registryPortKey) {
        this.registryPortKey = registryPortKey;
    }

    public void setExportNameKey(String exportNameKey) {
        this.exportNameKey = exportNameKey;
    }
}
