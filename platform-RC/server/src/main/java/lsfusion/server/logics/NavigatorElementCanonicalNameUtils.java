package lsfusion.server.logics;

//todo: merge with lsfusion.server.logics.PropertyCanonicalNameUtils into one CanonicalNamesUtils class
public class NavigatorElementCanonicalNameUtils {
    
    public static String createNavigatorElementCanonicalName(String namespace, String name) {
        return namespace + "." + name;
    }
}
