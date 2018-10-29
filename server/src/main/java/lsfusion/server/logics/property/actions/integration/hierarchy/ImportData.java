package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;

import java.sql.SQLException;

public interface ImportData {
    
    void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues, boolean isExclusive);

    void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive);
    
    Object genObject(ObjectEntity object) throws SQLException;
}
