package lsfusion.server.logics;

import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.entity.PropertyObjectEntity;

import java.util.List;

/**
 * User: DAle
 * Date: 19.11.13
 * Time: 9:27
 */

public interface PropertyDBNamePolicy {
    String createName(String namespaceName, String name, List<ResolveClassSet> signature);
    
    String transformToDBName(String canonicalName);
}
