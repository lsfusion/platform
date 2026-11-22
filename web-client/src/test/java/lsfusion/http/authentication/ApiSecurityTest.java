package lsfusion.http.authentication;

import lsfusion.http.provider.logics.LogicsProvider;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.Filter;
import java.lang.reflect.Proxy;

// The web server's security as security-config.xml declares it: the filters of its chains, for a test to send requests
// through
public abstract class ApiSecurityTest {

    protected static Filter security;

    @BeforeClass
    public static void loadSecurity() {
        XmlWebApplicationContext context = new XmlWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.setConfigLocation("file:src/main/webapp/WEB-INF/security-config.xml");
        Object logicsProvider = Proxy.newProxyInstance(LogicsProvider.class.getClassLoader(), new Class[]{LogicsProvider.class}, (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        });
        context.addBeanFactoryPostProcessor(beanFactory -> beanFactory.registerSingleton("logicsProvider", logicsProvider));
        context.refresh();
        security = context.getBean("springSecurityFilterChain", Filter.class);
    }
}
