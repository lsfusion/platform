package platform.gwt.base.server.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import platform.interop.RemoteLogicsInterface;

import javax.servlet.ServletContext;

public class ContextBusinessLogicsProvider<T extends RemoteLogicsInterface> extends SingleBusinessLogicsProvider<T> implements InitializingBean {

    @Autowired
    private ServletContext servletContext;

    private String serverHostKey = "serverHost";
    private String serverPortKey = "serverPort";

    @Override
    public void afterPropertiesSet() throws Exception {
        String serverHost = servletContext.getInitParameter(serverHostKey);
        String serverPort = servletContext.getInitParameter(serverPortKey);
        if (serverHost == null || serverPort == null) {
            throw new IllegalStateException(serverHostKey + " or " + serverPortKey + " parameters aren't set in web.xml");
        }

        setServerHost(serverHost);
        setServerPort(Integer.parseInt(serverPort));
    }

    public void setServerHostKey(String serverHostKey) {
        this.serverHostKey = serverHostKey;
    }

    public void setServerPortKey(String serverPortKey) {
        this.serverPortKey = serverPortKey;
    }
}
