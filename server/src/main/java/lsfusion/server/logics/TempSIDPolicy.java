package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: DAle
 * Date: 20.11.13
 * Time: 9:06
 */

public class TempSIDPolicy implements SIDPolicy {
    @Override
    public String createSID(String namespaceName, String name, List<ValueClass> signature, String oldName) {
        String actualName = oldName == null ? name : oldName;
        if (namespaceName == null) {
            return actualName;
        } else {
            return namespaceName + "_" + actualName;
        }
    }

    @Override
    public String createPropertyDrawSID(PropertyObjectEntity<?, ?> property) {
        if (property.property.getName() == null) {  // Для обратной совместимости. Для OBJVALUE, SELECTION пока берется внутренний SID   
            return property.property.getSID();
        } else {
            return OldSIDPolicy.createPropertyDrawSID(BaseUtils.nvl(property.property.getOldName(), property.property.getName()), property);
        }
    }
}
