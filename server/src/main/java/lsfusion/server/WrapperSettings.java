package lsfusion.server;

import lsfusion.server.logics.service.reflection.SaveReflectionPropertyActionProperty;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;

public class WrapperSettings {
    private static Settings wrappedSettings;

    public static Settings getSettings() {
        return wrappedSettings;
    }

    public static Object getProperty(String name) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return wrappedSettings != null ? BeanUtils.getProperty(wrappedSettings, name) : null;
    }

    public static void pushSettings(String name, String value) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, CloneNotSupportedException {
        wrappedSettings = Settings.copy();
        SaveReflectionPropertyActionProperty.setPropertyValue(wrappedSettings, name, value);
    }

    public static void popSettings() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        wrappedSettings = null;
    }
}