package platform.base;

import org.apache.log4j.Logger;
import platform.interop.remote.ServerSocketFactory;

import java.io.IOException;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;

public class ClassUtils {
    private final static Logger logger = Logger.getLogger(ClassUtils.class);

    public static String resolveName(Class<?> c, String name) {

        if (name == null) {
            return name;
        }
        if (!name.startsWith("/")) {
            while (c.isArray()) {
                c = c.getComponentType();
            }
            String baseName = c.getName();
            int index = baseName.lastIndexOf('.');
            if (index != -1) {
                name = baseName.substring(0, index).replace('.', '/')
                    +"/"+name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    public static void initRMICompressedSocketFactory() throws IOException {
        if (RMISocketFactory.getSocketFactory() == null) {
            RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
                public boolean failure(Exception ex) {
                    logger.error("Ошибка RMI: ", ex);
                    return true;
                }
            });

            RMISocketFactory.setSocketFactory(new ServerSocketFactory());
        }
    }
}
