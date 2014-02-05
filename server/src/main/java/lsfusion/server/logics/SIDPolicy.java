package lsfusion.server.logics;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.Property;

import java.util.List;

/**
 * User: DAle
 * Date: 19.11.13
 * Time: 9:27
 */

public interface SIDPolicy {
    String createSID(String namespaceName, String name, List<ValueClass> signature, String oldName);
    
    String createPropertyDrawSID(PropertyObjectEntity<?, ?> property);
}
