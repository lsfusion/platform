package lsfusion.server.logics;

public class ElementCanonicalNameUtils {
    
    public static String createCanonicalName(String namespace, String name) {
        return namespace + "." + name;
    }
}
