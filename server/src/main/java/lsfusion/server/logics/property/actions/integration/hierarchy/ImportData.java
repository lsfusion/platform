package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;

public interface ImportData {
    
    void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues);

    void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value);
    
    int getIndex(ObjectEntity object);
    void shiftIndex(ObjectEntity object, int index);
}
