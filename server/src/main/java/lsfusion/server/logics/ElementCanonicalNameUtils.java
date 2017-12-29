package lsfusion.server.logics;

public class ElementCanonicalNameUtils {
    
    public static String createCanonicalName(String namespace, String name) {
        return namespace + "." + name;
    }
    
    public static String getNamespace(String canonicalName) {
        int pointIndex = canonicalName.indexOf('.');
        assert pointIndex > 0; 
        return canonicalName.substring(0, pointIndex);
    }
    
    public static String getName(String canonicalName) {
        int pointIndex = canonicalName.indexOf('.');
        assert pointIndex > 0;
        return canonicalName.substring(pointIndex + 1);
    }
    
    public static String toSID(String canonicalName) {
        return canonicalName.replace('.', '_');
    }
}
