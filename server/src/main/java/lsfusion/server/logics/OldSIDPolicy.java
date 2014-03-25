package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.ArrayList;
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

    public static String createPropertyDrawSID(String name, PropertyObjectEntity<?, ?> property) {
        List<String> mapping = new ArrayList<String>();  
        for (PropertyInterface<?> pi : property.property.getOrderInterfaces()) {
            PropertyObjectInterfaceEntity obj = property.mapping.getObject(pi);
            assert obj instanceof ObjectEntity;
            mapping.add(((ObjectEntity) obj).getSID());
        }
        return PropertyDrawEntity.createSID(name, mapping);
    }
    
    @Override
    public String createPropertyDrawSID(PropertyObjectEntity<?, ?> property) {
        if (property.property.getName() == null) {  // Для обратной совместимости. Для OBJVALUE, SELECTION пока берется внутренний SID   
            return property.property.getSID(); 
        } else {
            return createPropertyDrawSID(property.property.getName(), property);
        }
    }
}
