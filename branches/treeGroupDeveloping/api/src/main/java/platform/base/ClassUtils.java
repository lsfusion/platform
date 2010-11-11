package platform.base;

public class ClassUtils {

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
}
