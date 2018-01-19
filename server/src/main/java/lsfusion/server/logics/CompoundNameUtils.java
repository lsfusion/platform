package lsfusion.server.logics;

public class CompoundNameUtils {
    public static final char DELIMITER = '.';

    public static class ParseException extends RuntimeException {
        ParseException(String message) {
            super(message);
        }
    }

    public static String createCompoundName(String namespace, String name) {
        String compoundName = "";
        if (namespace != null) {
            compoundName = namespace + DELIMITER;
        }
        return compoundName + name;
    } 
    
    public static String getName(String compoundName) {
        checkForCorrectness(compoundName);
        return getNameWithoutCheck(compoundName);
    }  
    
    private static String getNameWithoutCheck(String compoundName) {
        if (hasNamespace(compoundName)) {
            return compoundName.substring(delimiterPosition(compoundName) + 1);
        } else {
            return compoundName;
        }
    }
    
    public static String getNamespace(String compoundName) {
        checkForCorrectness(compoundName);
        return getNamespaceWithoutCheck(compoundName);
    }

    private static String getNamespaceWithoutCheck(String compoundName) {
        if (hasNamespace(compoundName)) {
            return compoundName.substring(0, delimiterPosition(compoundName));
        } else {
            return null;
        }
    }
    
    public static boolean hasNamespace(String compoundName) {
        return delimiterPosition(compoundName) > 0;
    }
    
    private static int delimiterPosition(String compoundName) {
        return compoundName.indexOf(DELIMITER);
    }
    
    public static boolean isCorrect(String compoundName) {
        if (hasNamespace(compoundName) && !isCorrectSimpleName(getNamespaceWithoutCheck(compoundName))) {
            return false;
        }
        return isCorrectSimpleName(getNameWithoutCheck(compoundName));
    }

    public static void checkForCorrectness(String compoundName) {
        if (hasNamespace(compoundName)) {
            checkForForbiddenSymbols("namespace", getNamespaceWithoutCheck(compoundName));
        }
        checkForForbiddenSymbols("name", getNameWithoutCheck(compoundName));
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
