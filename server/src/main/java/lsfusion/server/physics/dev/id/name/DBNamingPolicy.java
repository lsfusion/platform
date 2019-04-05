package lsfusion.server.physics.dev.id.name;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public interface DBNamingPolicy {
    String createActionOrPropertyDBName(String namespaceName, String name, List<ResolveClassSet> signature);
    
    String createTableDBName(String namespaceName, String name);

    String createAutoTableDBName(List<ValueClass> classes);
    
    String transformActionOrPropertyCNToDBName(String canonicalName);
    
    String transformTableCNToDBName(String canonicalName);
}
