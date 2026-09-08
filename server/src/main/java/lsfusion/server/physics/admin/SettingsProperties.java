package lsfusion.server.physics.admin;

import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

// the settings.<name> keys for every writable parameter of Settings : from the Environment (JVM parameters, then
// environment variables, where the key is spelled SETTINGS_<NAME>) and, when neither has it, from the properties
// files (conf/settings.properties over lsfusion.properties), with ${...} inside a value resolved over the same
// sources. The precedence and the resolution the ${settings.<name>:} placeholders had, without a list of the
// parameters to keep in lsfusion.xml
public class SettingsProperties implements FactoryBean<Map<String, String>>, EnvironmentAware {

    public static final String PREFIX = "settings.";

    // a parameter that installations have always set under another key (the desktop client's jnlp.singleInstance)
    private static final Map<String, String> ALIASES = Collections.singletonMap("singleInstance", "jnlp.singleInstance");

    private Environment environment;
    private Properties fileProperties;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setFileProperties(Properties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public Map<String, String> getObject() {
        Map<String, String> result = new HashMap<>();
        TreeSet<String> names = new TreeSet<>();
        PropertyResolver resolver = resolver();
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(Settings.class)) {
            if (descriptor.getWriteMethod() == null || !isParameterType(descriptor.getPropertyType())) // setProperties itself is a writable "properties"
                continue;
            String name = descriptor.getName();
            names.add(name);
            String value = resolver.getProperty(PREFIX + name);
            if (value == null && ALIASES.containsKey(name))
                value = resolver.getProperty(ALIASES.get(name));
            if (value != null)
                result.put(name, value);
        }

        // a key that matches no parameter would otherwise be ignored silently, which is how a misspelled one hides
        warnUnknown(fileProperties, names);
        warnUnknown(System.getProperties(), names);
        return result;
    }

    private static boolean isParameterType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Boolean.class || Number.class.isAssignableFrom(type);
    }

    // the Environment's sources first, the files last : what PropertySourcesPlaceholderConfigurer does with its local
    // properties, nested placeholders included
    private PropertyResolver resolver() {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source : ((ConfigurableEnvironment) environment).getPropertySources())
            sources.addLast(source);
        if (fileProperties != null)
            sources.addLast(new PropertiesPropertySource("lsfFileProperties", fileProperties));
        return new PropertySourcesPropertyResolver(sources);
    }

    private static void warnUnknown(Properties properties, TreeSet<String> names) {
        if (properties == null)
            return;
        for (String key : properties.stringPropertyNames())
            if (key.startsWith(PREFIX) && !names.contains(key.substring(PREFIX.length())))
                ServerLoggers.systemLogger.warn("Unknown setting '" + key + "' is ignored");
    }

    @Override
    public Class<?> getObjectType() {
        return Map.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
