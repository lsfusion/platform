package lsfusion.server.physics.dev.id.name;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public interface DBNamingPolicy {
    String createPropertyName(String namespaceName, String name, List<ResolveClassSet> signature);

    String createAutoTableName(List<ValueClass> classes);
    
    String transformPropertyCNToDBName(String canonicalName);
    
    String transformTableCNToDBName(String canonicalName);
}
