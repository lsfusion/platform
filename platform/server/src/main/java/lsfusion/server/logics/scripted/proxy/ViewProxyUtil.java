package lsfusion.server.logics.scripted.proxy;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import lsfusion.base.SoftHashMap;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.converters.FontConverter;
import lsfusion.server.logics.scripted.converters.KeyStrokeConverter;

import javax.swing.*;
import java.awt.*;

public class ViewProxyUtil {
    static {
        ConvertUtils.register(new FontConverter(), Font.class);
        ConvertUtils.register(new KeyStrokeConverter(), KeyStroke.class);
    }

    private static final SoftHashMap<Object, ViewProxy> viewProxies = new SoftHashMap<Object, ViewProxy>();

    /**
     * not thread-safe
     */
    public static void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
        if (propertyReceiver == null) {
            throw new RuntimeException("object is undefined");
        }

        ViewProxy viewProxy = null;
        try {
            viewProxy = getViewProxy(propertyReceiver);
        } catch (Exception e) {
            throw new RuntimeException("object doesn't support setting properties");
        }

        if (!PropertyUtils.isWriteable(viewProxy, propertyName)) {
            throw new RuntimeException("property doesn't exist");
        }

        try {
            BeanUtils.setProperty(viewProxy, propertyName, propertyValue);
        } catch (Exception e) {
            throw new RuntimeException("property can't be set: " + e.getMessage());
        }
    }

    public static ViewProxy getViewProxy(Object target) {
        if (target == null) {
            return null;
        }

        ViewProxy proxy = viewProxies.get(target);
        if (proxy == null) {
            proxy = ViewProxyFactory.getInstance().createViewProxy(target);
            viewProxies.put(target, proxy);
        }

        return proxy;
    }
}
