package lsfusion.base.classloader;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReadUsedClassLoader extends ClassLoader {

    private static Map<String, byte[]> classes;

    public ReadUsedClassLoader(ClassLoader originalClassloader) {
        super(originalClassloader);
        classes = new HashMap<>();
    }

    public static Map<String, byte[]> getClasses() {
        return classes;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream resourceAsStream = super.getResourceAsStream(name);
        if (resourceAsStream != null) { //call from the server. Save classes that are used in .jrxml
            try {
                if (classIsNotOnTheClient(name))  //save classes that will not be in the client's classpath
                    classes.put(name, IOUtils.toByteArray(resourceAsStream));

            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        return super.getResourceAsStream(name);
    }

    private static final Map<String, Boolean> classesOnTheClient = new LinkedHashMap<>(); // Order is important.
    static {
        // Map store string that the package name starts with and permission to add or not to add it.
        // Order is important because there may be overlaps in the package names.
        classesOnTheClient.put("java/sql", true); //add classes from this package as in java 9 and above this package is not in the base module and not available when compiling reports on the desktop client when running from .jnlp because javawebstart uses its own classloader
        classesOnTheClient.put("lsfusion/base", false);
        classesOnTheClient.put("java", false);
        classesOnTheClient.put("net/sf", false);
    }

    private boolean classIsNotOnTheClient(String name) {
        for (Map.Entry<String, Boolean> stringBooleanEntry : classesOnTheClient.entrySet()) {
            if (name.startsWith(stringBooleanEntry.getKey()))
                return stringBooleanEntry.getValue();
        }

        return true;
    }
}
