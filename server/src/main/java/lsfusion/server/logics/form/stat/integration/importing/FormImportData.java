package lsfusion.server.logics.form.stat.integration.importing;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.stat.integration.hierarchy.ImportData;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class FormImportData implements ImportData {

    private ImMap<GroupObjectEntity, ImSet<FilterEntity>> groupFixedFilters;
    private final DataSession session; // to add objects

    public FormImportData(FormEntity form, ExecutionContext<PropertyInterface> context) {
        groupFixedFilters = form.getImportFixedFilters();
        session = context.getSession();
    }

    private final MExclMap<CalcPropertyObjectEntity, MMap<ImMap<ObjectEntity, Object>, Object>> properties = MapFact.mExclMap();
    
    public final ImMap<CalcPropertyObjectEntity, ImMap<ImMap<ObjectEntity, Object>, Object>> result() {
        return MapFact.immutableMapMap(properties);
    }
    
    public void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues, boolean isExclusive) {
        if(group == null)
            return;
        
        ImSet<FilterEntity> groupFilters = groupFixedFilters.get(group);
        if(groupFilters != null) {
            for(FilterEntity<?> filter : groupFilters) {
                CalcPropertyObjectEntity<?> importProperty = filter.getImportProperty();
                addProperty(importProperty, upKeyValues, ((DataClass)importProperty.property.getType()).getDefaultValue(), isExclusive);
            }
        }
    }

    public void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive) {
        addProperty(entity.getImportProperty(), upKeyValues, value, isExclusive);
    }

    public void addProperty(CalcPropertyObjectEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive) {
        ImMap<ObjectEntity, Object> paramObjects = upKeyValues.filterIncl(entity.getObjectInstances());
        addProperty(entity, isExclusive).add(paramObjects, value);
    }

    public MMap<ImMap<ObjectEntity, Object>, Object> addProperty(CalcPropertyObjectEntity<?> entity, boolean isExclusive) {
        MMap<ImMap<ObjectEntity, Object>, Object> propertyValues = properties.get(entity);
        if(propertyValues == null) {
            propertyValues = MapFact.mMap(isExclusive);
            properties.exclAdd(entity, propertyValues);
        }
        return propertyValues;
    }
    
    private final MAddMap<ObjectEntity, Integer> indexes = MapFact.mAddMap(MapFact.<ObjectEntity, Integer>override()); // end-to-end numeration

    private final MExclMap<ObjectEntity, MExclSet<Long>> addedObjects = MapFact.mExclMap();

    public ImMap<ObjectEntity, ImSet<Long>> resultAddedObjects() {
        return MapFact.immutableMapExcl(addedObjects);
    }

    @Override
    public Object genObject(ObjectEntity object) throws SQLException {
        // object
        if(object.baseClass instanceof ConcreteCustomClass) {
            long addedObject = session.generateID();

            MExclSet<Long> objects = addedObjects.get(object);
            if(objects == null) {
                objects = SetFact.mExclSet();
                addedObjects.exclAdd(object, objects);
            }
            objects.exclAdd(addedObject);

            return addedObject;
        }

        // integer
        int result = BaseUtils.nvl(indexes.get(object), 0);
        indexes.add(object, result + 1);
        return ((DataClass)object.baseClass).read(result);
    }
}
