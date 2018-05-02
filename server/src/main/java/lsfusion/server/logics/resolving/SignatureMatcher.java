package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.List;

public class SignatureMatcher {

    public static boolean isCompatible(List<ResolveClassSet> interfaceClasses, List<ResolveClassSet> paramClasses, boolean strict, boolean falseImplicitClass) {
        assert interfaceClasses != null;
        if (paramClasses == null) {
            return true;
        }
        if (interfaceClasses.size() != paramClasses.size()) {
            return false;
        }

        for (int i = 0, size = interfaceClasses.size(); i < size; i++) {
            if (!isClassesCompatible(interfaceClasses.get(i), paramClasses.get(i), strict, falseImplicitClass)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClassesCompatible(ResolveClassSet interfaceClass, ResolveClassSet paramClass, boolean strict, boolean falseImplicitClass) {
        if (interfaceClass != null) {
            if (paramClass != null && !interfaceClass.containsAll(paramClass, !strict) ||
                paramClass == null && falseImplicitClass) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isSoftCompatible(List<ResolveClassSet> interfaceClasses, List<ResolveClassSet> paramClasses) {
        assert interfaceClasses != null;
        if (paramClasses == null) {
            return true;
        }
        if (interfaceClasses.size() != paramClasses.size()) {
            return false;
        }

        for (int i = 0; i < interfaceClasses.size(); i++) {
            if (!isClassesSoftCompatible(interfaceClasses.get(i), paramClasses.get(i))) {
                return false; 
            }
        }
        return true;
    }
 
    public static boolean isClassesSoftCompatible(ResolveClassSet interfaceClass, ResolveClassSet paramClass) {
        return interfaceClass == null || paramClass == null || !(interfaceClass.and(paramClass)).isEmpty(); 
    }

    public static boolean isEqualsCompatible(List<ResolveClassSet> interfaceClasses, List<ResolveClassSet> paramClasses) {
        assert interfaceClasses != null && paramClasses != null;
        if (interfaceClasses.size() != paramClasses.size()) {
            return false;
        }

        for (int i = 0; i < interfaceClasses.size(); i++) {
            if (!isClassesEqualsCompatible(interfaceClasses.get(i), paramClasses.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClassesEqualsCompatible(ResolveClassSet interfaceClass, ResolveClassSet paramClass) {
        if (interfaceClass == null || paramClass == null) return true;
        
        return interfaceClass.equalsCompatible(paramClass);
    }

}
