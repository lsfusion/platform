package lsfusion.base.classloader;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
                if (!name.startsWith("lsfusion/base") && !name.startsWith("java") && !name.startsWith("net/sf")) //save classes that will not be in the client's classpath
                    classes.put(name, IOUtils.toByteArray(resourceAsStream));
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        return super.getResourceAsStream(name);
    }
}
