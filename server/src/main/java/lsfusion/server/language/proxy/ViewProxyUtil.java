package lsfusion.server.language.proxy;

import lsfusion.base.col.heavy.SoftHashMap;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.converters.FontInfoConverter;
import lsfusion.server.language.converters.KeyStrokeConverter;
import lsfusion.server.physics.dev.debug.DebugInfo;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;

import java.util.function.Supplier;

public class ViewProxyUtil {
    static {
        ConvertUtils.register(new FontInfoConverter(), FontInfo.class);
        ConvertUtils.register(new KeyStrokeConverter(), ScriptingLogicsModule.KeyStrokeOptions.class);
        BeanUtilsBean.getInstance().getConvertUtils().register(true, false, -1); //all converters will throw exceptions
    }

    private static final SoftHashMap<Object, ViewProxy> viewProxies = new SoftHashMap<>();

    /**
     * not thread-safe
     */
    public static void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue, Supplier<DebugInfo.DebugPoint> debugPoint) {
        if (propertyReceiver == null) {
            throw new RuntimeException("object is undefined");
        }

        ViewProxy viewProxy;
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

    private static ViewProxy getViewProxy(Object target) {
        if (target == null) {
            return null;
        }

        ViewProxy proxy;
        synchronized (viewProxies) {
            proxy = viewProxies.get(target);
            if (proxy == null) {
                proxy = ViewProxyFactory.getInstance().createViewProxy(target);
                viewProxies.put(target, proxy);
            }
        }

        return proxy;
    }
}
