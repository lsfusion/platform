package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;

public interface ExportData {

    Type getType(PropertyDrawEntity<?> entity);

    // different interfaces for optimization
    
    // hierarchical
    Iterable<ImMap<ObjectEntity, Object>> getObjects(GroupObjectEntity entity, ImMap<ObjectEntity, Object> upKeyValues);
    Object getProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> keys);

    // plain
    ImOrderSet<ImMap<ObjectEntity, Object>> getRows(GroupObjectEntity entity);
}
