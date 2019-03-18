package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

public interface ExportData {

    Type getType(PropertyDrawEntity<?> entity);

    // different interfaces for optimization
    
    // hierarchical
    ImList<ImMap<ObjectEntity, Object>> getObjects(GroupObjectEntity entity, ImMap<ObjectEntity, Object> upKeyValues);
    Object getProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> keys);

    // plain
    ImOrderSet<ImMap<ObjectEntity, Object>> getRows(GroupObjectEntity entity);
}
