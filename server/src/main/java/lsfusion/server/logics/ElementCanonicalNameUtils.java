package lsfusion.server.logics;

import org.apache.commons.lang.StringUtils;

public class ElementCanonicalNameUtils {
    
    public static String createCanonicalName(String namespace, String name) {
        return namespace + "." + name;
    }
    
    public static String extractNamespace(String canonicalName) {
        assert StringUtils.countMatches(canonicalName, ".") == 1;
        return canonicalName.substring(0, canonicalName.indexOf('.'));
    }
    
    public static String extractName(String canonicalName) {
        assert StringUtils.countMatches(canonicalName, ".") == 1;
        return canonicalName.substring(canonicalName.indexOf('.') + 1);
    }
}
