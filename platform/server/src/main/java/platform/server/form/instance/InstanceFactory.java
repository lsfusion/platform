package platform.server.form.instance;

import org.apache.poi.ss.formula.functions.T;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.filter.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcPropertyRevImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;

public class InstanceFactory {

    public InstanceFactory(PropertyObjectInterfaceInstance computer, DataObject connection) {
        this.computer = computer;
        this.connection = connection;
    }

    private final MAddExclMap<ObjectEntity, ObjectInstance> objectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<GroupObjectEntity, GroupObjectInstance> groupInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<TreeGroupEntity, TreeGroupInstance> treeInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<PropertyObjectEntity, PropertyObjectInstance> propertyObjectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<PropertyDrawEntity, PropertyDrawInstance> propertyDrawInstances = MapFact.mSmallStrongMap();


    public ObjectInstance getInstance(ObjectEntity entity) {
        if (!objectInstances.containsKey(entity)) {
            objectInstances.exclAdd(entity, entity.baseClass.newInstance(entity));
        }
        return objectInstances.get(entity);
    }

    public GroupObjectInstance getInstance(GroupObjectEntity entity) {

        if (entity == null) {
            return null;
        }

        if (!groupInstances.containsKey(entity)) {

            ImOrderSet<ObjectInstance> objects = entity.getOrderObjects().mapOrderSetValues(new GetValue<ObjectInstance, ObjectEntity>() { // последействие есть, но "статичное"
                public ObjectInstance getMapValue(ObjectEntity value) {
                    return getInstance(value);
                }
            });

            ImMap<ObjectInstance, CalcPropertyObjectInstance> parentInstances = null;
            if(entity.isParent !=null) {
                parentInstances = entity.isParent.mapKeyValues(new GetValue<ObjectInstance, ObjectEntity>() {
                    public ObjectInstance getMapValue(ObjectEntity value) {
                        return getInstance(value);
                    }}, new GetValue<CalcPropertyObjectInstance, CalcPropertyObjectEntity<?>>() {
                    public CalcPropertyObjectInstance<?> getMapValue(CalcPropertyObjectEntity value) {
                        return getInstance(value);
                    }});
            }

            groupInstances.exclAdd(entity, new GroupObjectInstance(entity, objects, entity.propertyBackground != null ? getInstance(entity.propertyBackground) : null,
                    entity.propertyForeground != null ? getInstance(entity.propertyForeground) : null, parentInstances,
                    entity.readFilterProperty !=null ? getInstance(entity.readFilterProperty) : null));
        }

        return groupInstances.get(entity);
    }

    public TreeGroupInstance getInstance(TreeGroupEntity entity) {

        if (entity == null) {
            return null;
        }

        if (!treeInstances.containsKey(entity)) {

            ImOrderSet<GroupObjectInstance> groups = entity.getGroups().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() { // тут как бы с последействием, но "статичным"
                public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                    return getInstance(value);
                }
            });
            treeInstances.exclAdd(entity, new TreeGroupInstance(entity, groups));
        }

        return treeInstances.get(entity);
    }

    private <P extends PropertyInterface> ImMap<P, PropertyObjectInterfaceInstance> getInstanceMap(PropertyObjectEntity<P, ?> entity) {
        return entity.mapping.mapValues(new GetValue<PropertyObjectInterfaceInstance, PropertyObjectInterfaceEntity>() {
            public PropertyObjectInterfaceInstance getMapValue(PropertyObjectInterfaceEntity value) {
                return value.getInstance(InstanceFactory.this);
            }});
    }

    public <P extends PropertyInterface> CalcPropertyObjectInstance<P> getInstance(CalcPropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new CalcPropertyObjectInstance<P>(entity.property, getInstanceMap(entity)));

        return (CalcPropertyObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    private <P extends PropertyInterface> ImRevMap<P, ObjectInstance> getInstanceMap(CalcPropertyRevImplement<P, ObjectEntity> entity) {
        return entity.mapping.mapRevValues(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return InstanceFactory.this.getInstance(value);
            }});
    }

    public <P extends PropertyInterface> CalcPropertyRevImplement<P, ObjectInstance> getInstance(CalcPropertyRevImplement<P, ObjectEntity> entity) {
        return new CalcPropertyRevImplement<P, ObjectInstance>(entity.property, getInstanceMap(entity));
    }

        // временно
    public <P extends PropertyInterface> PropertyObjectInstance<P, ?> getInstance(PropertyObjectEntity<P, ?> entity) {
        if(entity instanceof CalcPropertyObjectEntity)
            return getInstance((CalcPropertyObjectEntity<P>)entity);
        else
            return getInstance((ActionPropertyObjectEntity<P>)entity);
    }

    public <P extends PropertyInterface> ActionPropertyObjectInstance<P> getInstance(ActionPropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new ActionPropertyObjectInstance<P>(entity.property, getInstanceMap(entity)));

        return (ActionPropertyObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    public <T extends PropertyInterface> PropertyDrawInstance getInstance(PropertyDrawEntity<T> entity) {

        if (!propertyDrawInstances.containsKey(entity)) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = entity.getColumnGroupObjects().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() {
                public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                    return getInstance(value);
                }
            });

            propertyDrawInstances.exclAdd(entity, new PropertyDrawInstance<T>(
                    entity,
                    getInstance(entity.propertyObject),
                    getInstance(entity.toDraw),
                    columnGroupObjects,
                    entity.propertyCaption == null ? null : getInstance(entity.propertyCaption),
                    entity.propertyReadOnly == null ? null : getInstance(entity.propertyReadOnly),
                    entity.propertyFooter == null ? null : getInstance(entity.propertyFooter),
                    entity.propertyBackground == null ? null : getInstance(entity.propertyBackground),
                    entity.propertyForeground == null ? null : getInstance(entity.propertyForeground)));
        }

        return propertyDrawInstances.get(entity);
    }

    public final PropertyObjectInterfaceInstance computer;
    public DataObject connection;

    public <P extends PropertyInterface> FilterInstance getInstance(CompareFilterEntity<P> entity) {
        return new CompareFilterInstance<P>(getInstance(entity.property), entity.compare, entity.value.getInstance(this), entity.resolveAdd);
    }

    public <P extends PropertyInterface> FilterInstance getInstance(IsClassFilterEntity<P> entity) {
        return new IsClassFilterInstance<P>(getInstance(entity.property), entity.isClass, entity.resolveAdd);
    }

    public <P extends PropertyInterface> FilterInstance getInstance(NotNullFilterEntity<P> entity) {
        return new NotNullFilterInstance<P>(getInstance(entity.property), entity.checkChange, entity.resolveAdd);
    }

    public FilterInstance getInstance(NotFilterEntity entity) {
        return new NotFilterInstance(entity.filter.getInstance(this));
    }

    public OrFilterInstance getInstance(OrFilterEntity entity) {
        return new OrFilterInstance(entity.op1.getInstance(this), entity.op2.getInstance(this));
    }

    public AndFilterInstance getInstance(AndFilterEntity entity) {
        return new AndFilterInstance(entity.op1.getInstance(this), entity.op2.getInstance(this));
    }

    public RegularFilterGroupInstance getInstance(RegularFilterGroupEntity entity) {

        RegularFilterGroupInstance group = new RegularFilterGroupInstance(entity);

        for (RegularFilterEntity filter : entity.filters) {
            group.addFilter(getInstance(filter));
        }

        return group;
    }

    public RegularFilterInstance getInstance(RegularFilterEntity entity) {
        return new RegularFilterInstance(entity, entity.filter.getInstance(this));
    }
}
