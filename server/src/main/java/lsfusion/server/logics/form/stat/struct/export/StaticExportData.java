package lsfusion.server.logics.form.stat.struct.export;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.StaticKeyData;
import lsfusion.server.logics.form.stat.StaticPropertyData;
import lsfusion.server.logics.form.stat.struct.hierarchy.ExportData;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.util.Map;

public class StaticExportData implements ExportData {

    private final Map<GroupObjectEntity, StaticKeyData> keyData;
    private final StaticPropertyData<PropertyDrawEntity> propData;

    public StaticExportData(Map<GroupObjectEntity, StaticKeyData> keyData, StaticPropertyData<PropertyDrawEntity> propData) {
        this.keyData = keyData;
        this.propData = propData;
    }

    // map upper objects on inner objects (according to hierarchy)
    // optimization
    private ImMap<ImMap<ObjectEntity, Object>, ImList<ImMap<ObjectEntity, Object>>> calcMapObjects(GroupObjectEntity entity, final ImSet<ObjectEntity> upObjects) {
        return keyData.get(entity).data.groupList(new BaseUtils.Group<ImMap<ObjectEntity, Object>, ImMap<ObjectEntity, Object>>() {
            public ImMap<ObjectEntity, Object> group(ImMap<ObjectEntity, Object> key) {
                return key.filterIncl(upObjects);
            }
        });
    }
    final MAddExclMap<GroupObjectEntity, ImMap<ImMap<ObjectEntity, Object>, ImList<ImMap<ObjectEntity, Object>>>> cacheMapObjects = MapFact.mAddExclMap();
    private ImMap<ImMap<ObjectEntity, Object>, ImList<ImMap<ObjectEntity, Object>>> getMapObjects(GroupObjectEntity entity, ImMap<ObjectEntity, Object> upKeyValues) {
        ImMap<ImMap<ObjectEntity, Object>, ImList<ImMap<ObjectEntity, Object>>> innerRows = cacheMapObjects.get(entity);
        if(innerRows == null) {
            innerRows = calcMapObjects(entity, upKeyValues.keys());
            cacheMapObjects.exclAdd(entity, innerRows);
        }
        return innerRows;
    }
    @ManualLazy
    public ImList<ImMap<ObjectEntity, Object>> getObjects(GroupObjectEntity entity, ImMap<ObjectEntity, Object> upKeys) {
        ImMap<ImMap<ObjectEntity, Object>, ImList<ImMap<ObjectEntity, Object>>> innerRows = getMapObjects(entity, upKeys);
        ImList<ImMap<ObjectEntity, Object>> innerRowValues = innerRows.get(upKeys);
        if(innerRowValues != null)
            return innerRowValues; 
        return ListFact.EMPTY();
    }

    @Override
    public Type getType(PropertyDrawEntity<?> entity) {
        return propData.types.get(entity);
    }

    @Override
    public Object getProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> keys) {
        return StaticPropertyData.getProperty(propData, entity, keys);
    }

    @Override
    public ImOrderSet<ImMap<ObjectEntity, Object>> getRows(GroupObjectEntity entity) {
        return keyData.get(entity).data;
    }
}
