package lsfusion.server.logics.property.actions.integration.importing;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.logics.property.actions.integration.hierarchy.ImportData;

public class FormImportData implements ImportData {

    private ImMap<GroupObjectEntity, ImSet<FilterEntity>> groupFixedFilters;

    public FormImportData(FormEntity form) {
        groupFixedFilters = form.getImportFixedFilters();
    }

    private final MExclMap<CalcPropertyObjectEntity, MMap<ImMap<ObjectEntity, Object>, Object>> properties = MapFact.mExclMap();
    
    public final ImMap<CalcPropertyObjectEntity, ImMap<ImMap<ObjectEntity, Object>, Object>> result() {
        return MapFact.immutableMapMap(properties);
    }
    
    public void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues) {
        if(group == null)
            return;
        
        ImSet<FilterEntity> groupFilters = groupFixedFilters.get(group);
        if(groupFilters != null) {
            for(FilterEntity<?> filter : groupFilters) {
                CalcPropertyObjectEntity<?> importProperty = filter.getImportProperty();
                addProperty(importProperty, upKeyValues, ((DataClass)importProperty.property.getType()).getDefaultValue());
            }
        }
    }

    public void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value) {
        addProperty(entity.getImportProperty(), upKeyValues, value);
    }

    public void addProperty(CalcPropertyObjectEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value) {
        ImMap<ObjectEntity, Object> paramObjects = upKeyValues.filterIncl(entity.getObjectInstances());

        MMap<ImMap<ObjectEntity, Object>, Object> propertyValues = properties.get(entity);
        if(propertyValues == null) {
            propertyValues = MapFact.mMap(MapFact.<ImMap<ObjectEntity, Object>, Object>override());
            properties.exclAdd(entity, propertyValues);
        }
        propertyValues.add(paramObjects, value);
    }
    
    private final MAddMap<ObjectEntity, Integer> indexes = MapFact.mAddMap(new SymmAddValue<ObjectEntity, Integer>() {
        public Integer addValue(ObjectEntity key, Integer prevValue, Integer newValue) {
            return prevValue + newValue;
        }
    }); // end-to-end numeration

    @Override
    public int getIndex(ObjectEntity object) {
        Integer result = indexes.get(object);
        return result == null ? 0 : result;
    }

    @Override
    public void shiftIndex(ObjectEntity object, int count) {
        indexes.add(object, count);
    }
}
