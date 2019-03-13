package lsfusion.server.logics.form.stat.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.sql.SQLException;

public interface ImportData {
    
    void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues, boolean isExclusive);

    void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive);
    
    Object genObject(ObjectEntity object) throws SQLException;
}
