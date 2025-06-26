package lsfusion.server.logics.form.stat.struct.imports;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.stat.struct.hierarchy.ImportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FormImportData implements ImportData {

    private ImMap<GroupObjectEntity, ImSet<FilterEntity>> groupFixedFilters;
    private final DataSession session; // to add objects

    public FormImportData(FormEntity form, ExecutionContext<PropertyInterface> context) {
        groupFixedFilters = form.getImportFixedFilters();
        session = context.getSession();
    }

    private final Map<PropertyObjectEntity, MMap<ImMap<ObjectEntity, Object>, Object>> properties = MapFact.mAddRemoveMap();
    //can be more than one PropertyDrawEntity / FilterEntity for PropertyObjectEntity, prior will be the last
    private Map<PropertyObjectEntity, Set<Object>> filterOrPropertyEntities = MapFact.mAddRemoveMap();
    private Map<PropertyObjectEntity, Object> currentFilterOrPropertyEntities = MapFact.mAddRemoveMap();

    public final Map<PropertyObjectEntity, MMap<ImMap<ObjectEntity, Object>, Object>> result() {
        return properties;
    }
    
    public void addObject(GroupObjectEntity group, ImMap<ObjectEntity, Object> upKeyValues, boolean isExclusive) {
        if(group == null)
            return;
        
        ImSet<FilterEntity> groupFilters = groupFixedFilters.get(group);
        if(groupFilters != null) {
            for(FilterEntity<?> filter : groupFilters) {
                PropertyObjectEntity<?> importProperty = filter.getProperty();
                addProperty(filter, importProperty, upKeyValues, ((DataClass)importProperty.property.getType()).getDefaultValue(), isExclusive);
            }
        }
    }

    public void addProperty(PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive) {
        addProperty(entity, entity.getImportProperty(), upKeyValues, value, isExclusive);
    }

    public void addProperty(Object filterOrPropertyEntity, PropertyObjectEntity<?> entity, ImMap<ObjectEntity, Object> upKeyValues, Object value, boolean isExclusive) {
        ImMap<ObjectEntity, Object> paramObjects = upKeyValues.filterIncl(entity.getObjectInstances());

        MMap<ImMap<ObjectEntity, Object>, Object> propertyValues = addProperty(filterOrPropertyEntity, entity, isExclusive);
        if(filterOrPropertyEntity.equals(currentFilterOrPropertyEntities.get(entity))) {
            propertyValues.add(paramObjects, value);
        }
    }

    public MMap<ImMap<ObjectEntity, Object>, Object> addProperty(Object filterOrPropertyEntity, PropertyObjectEntity<?> entity, boolean isExclusive) {
        Set<Object> fop = filterOrPropertyEntities.getOrDefault(entity, new HashSet<>());
        if (!fop.contains(filterOrPropertyEntity)) {
            fop.add(filterOrPropertyEntity);
            filterOrPropertyEntities.put(entity, fop);
            currentFilterOrPropertyEntities.put(entity, filterOrPropertyEntity);
            properties.remove(entity);
        }

        MMap<ImMap<ObjectEntity, Object>, Object> propertyValues = properties.get(entity);
        if (propertyValues == null) {
            propertyValues = MapFact.mMap(isExclusive);
            properties.put(entity, propertyValues);
        }
        return propertyValues;
    }
    
    private final MAddMap<ObjectEntity, Integer> indexes = MapFact.mAddMap(MapFact.override()); // end-to-end numeration

    private final MExclMap<ObjectEntity, MExclSet<Long>> addedObjects = MapFact.mExclMap();

    public ImMap<ObjectEntity, ImSet<Long>> resultAddedObjects() {
        return MapFact.immutableMapExcl(addedObjects);
    }

    @Override
    public Object genObject(ObjectEntity object) throws SQLException, SQLHandledException {
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
        return ((IntegralClass)object.baseClass).readNumber(result);
    }
}
