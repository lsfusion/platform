package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.List;

public interface PropertyDBNamePolicy {
    String createName(String namespaceName, String name, List<ResolveClassSet> signature);
    
    String transformToDBName(String canonicalName);
}
