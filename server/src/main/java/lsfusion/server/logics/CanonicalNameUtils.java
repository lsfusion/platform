package lsfusion.server.logics;

public class CanonicalNameUtils {
    public static final char DELIMITER = '.';

    public static class ParseException extends RuntimeException {
        ParseException(String message) {
            super(message);
        }
    }

    public static String createCanonicalName(String namespace, String name) {
        return namespace + DELIMITER + name;
    }

    public static String toSID(String canonicalName) {
        return canonicalName.replace(DELIMITER, '_');
    }
    
    public static String getNamespace(String canonicalName) {
        checkForCorrectness(canonicalName);
        return getNamespaceWithoutCheck(canonicalName);
    }
    
    private static String getNamespaceWithoutCheck(String canonicalName) {
        return canonicalName.substring(0, delimiterPosition(canonicalName));
    } 
    
    public static String getName(String canonicalName) {
        checkForCorrectness(canonicalName);
        return getNameWithoutCheck(canonicalName);        
    }

    private static String getNameWithoutCheck(String canonicalName) {
        return canonicalName.substring(delimiterPosition(canonicalName) + 1);
    }
    
    private static int delimiterPosition(String canonicalName) {
        return canonicalName.indexOf(DELIMITER);
    }
    
    public static boolean isCorrect(String canonicalName) {
        return delimiterPosition(canonicalName) > 0 
                && isCorrectSimpleName(getNamespaceWithoutCheck(canonicalName)) 
                && isCorrectSimpleName(getNameWithoutCheck(canonicalName));
    }

    public static void checkForCorrectness(String canonicalName) {
        if (delimiterPosition(canonicalName) < 0) {
            throw new ParseException("namespace is missing");
        } 
        
        checkForForbiddenSymbols("namespace", getNamespaceWithoutCheck(canonicalName));
        checkForForbiddenSymbols("name", getNameWithoutCheck(canonicalName));
    }
    
    private static void checkForForbiddenSymbols(String partName, String name) {
        if (!isCorrectSimpleName(name)) {
            throw new ParseException(String.format("%s '%s' contains forbidden symbols", partName, name));
        }
    }
    
    private static boolean isCorrectSimpleName(String name) {
        return name.matches("[a-zA-Z0-9_]+");
    }
}
