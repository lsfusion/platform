package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.Property;

import java.util.List;

/**
 * User: DAle
 * Date: 20.11.13
 * Time: 10:58
 */

public class OldSIDPolicy implements SIDPolicy {
    @Override
    public String createSID(String namespaceName, String name, List<ValueClass> signature, String oldName) {
        if (namespaceName == null) {
            return name;
        } else {
            return namespaceName + "_" + name;
        }
    }

    @Override
    public String createPropertyDrawSID(Property property) {
        return BaseUtils.nvl(property.getName(), property.getSID());  
    }
}
