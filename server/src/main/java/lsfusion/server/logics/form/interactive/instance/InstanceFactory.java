package lsfusion.server.logics.form.interactive.instance;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.TreeGroupInstance;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.ActionOrPropertyObjectInstance;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.ActionObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InstanceFactory {

    public InstanceFactory() {
    }

    private final MAddExclMap<ObjectEntity, ObjectInstance> objectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<GroupObjectEntity, GroupObjectInstance> groupInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<TreeGroupEntity, TreeGroupInstance> treeInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<ActionOrPropertyObjectEntity, ActionOrPropertyObjectInstance> propertyObjectInstances = MapFact.mSmallStrongMap();
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

            ImMap<ObjectInstance, PropertyObjectInstance> parentInstances = null;
            if(entity.isParent !=null) {
                parentInstances = entity.isParent.mapKeyValues(new GetValue<ObjectInstance, ObjectEntity>() {
                    public ObjectInstance getMapValue(ObjectEntity value) {
                        return getInstance(value);
                    }}, new GetValue<PropertyObjectInstance, PropertyObjectEntity<?>>() {
                    public PropertyObjectInstance<?> getMapValue(PropertyObjectEntity value) {
                        return getInstance(value);
                    }});
            }

            groupInstances.exclAdd(entity, new GroupObjectInstance(entity, objects, entity.propertyBackground != null ? getInstance(entity.propertyBackground) : null,
                    entity.propertyForeground != null ? getInstance(entity.propertyForeground) : null, parentInstances,
                    getInstance(entity.getProperties())));
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

    private <P extends PropertyInterface> ImMap<P, ObjectInstance> getInstanceMap(ActionOrPropertyObjectEntity<P, ?> entity) {
        return entity.mapping.mapValues(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return value.getInstance(InstanceFactory.this);
            }});
    }

    public <P extends PropertyInterface> PropertyObjectInstance<P> getInstance(PropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new PropertyObjectInstance<>(entity.property, getInstanceMap(entity)));

        return (PropertyObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    private <P extends PropertyInterface> ImRevMap<P, ObjectInstance> getInstanceMap(PropertyRevImplement<P, ObjectEntity> entity) {
        return entity.mapping.mapRevValues(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return InstanceFactory.this.getInstance(value);
            }});
    }

    public <T, P extends PropertyInterface> ImMap<T, PropertyRevImplement<P, ObjectInstance>> getInstance(ImMap<T, PropertyRevImplement<P, ObjectEntity>> entities) {
        return entities.mapValues(new GetValue<PropertyRevImplement<P, ObjectInstance>, PropertyRevImplement<P, ObjectEntity>>() {
            public PropertyRevImplement<P, ObjectInstance> getMapValue(PropertyRevImplement<P, ObjectEntity> entity) {
                return new PropertyRevImplement<>(entity.property, getInstanceMap(entity));
            }});
    }

        // временно
    public <P extends PropertyInterface> ActionOrPropertyObjectInstance<P, ?> getInstance(ActionOrPropertyObjectEntity<P, ?> entity) {
        if(entity instanceof PropertyObjectEntity)
            return getInstance((PropertyObjectEntity<P>)entity);
        else
            return getInstance((ActionObjectEntity<P>)entity);
    }

    public <P extends PropertyInterface> ActionObjectInstance<P> getInstance(ActionObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new ActionObjectInstance<>(entity.property, getInstanceMap(entity)));

        return (ActionObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    public <T extends PropertyInterface> PropertyDrawInstance getInstance(PropertyDrawEntity<T> entity) {

        if (!propertyDrawInstances.containsKey(entity)) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = entity.getColumnGroupObjects().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() {
                public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                    return getInstance(value);
                }
            });

            propertyDrawInstances.exclAdd(entity, new PropertyDrawInstance<>(
                    entity,
                    getInstance(entity.getValueProperty()),
                    getInstance(entity.toDraw),
                    columnGroupObjects,
                    entity.propertyCaption == null ? null : getInstance(entity.propertyCaption),
                    entity.propertyShowIf == null ? null : getInstance(entity.propertyShowIf),
                    entity.propertyReadOnly == null ? null : getInstance(entity.propertyReadOnly),
                    entity.propertyFooter == null ? null : getInstance(entity.propertyFooter),
                    entity.propertyBackground == null ? null : getInstance(entity.propertyBackground),
                    entity.propertyForeground == null ? null : getInstance(entity.propertyForeground)));
        }

        return propertyDrawInstances.get(entity);
    }

    public RegularFilterGroupInstance getInstance(RegularFilterGroupEntity entity) {

        RegularFilterGroupInstance group = new RegularFilterGroupInstance(entity);

        for (RegularFilterEntity filter : entity.getFiltersList()) {
            group.addFilter(getInstance(filter));
        }

        return group;
    }

    public RegularFilterInstance getInstance(RegularFilterEntity entity) {
        return new RegularFilterInstance(entity, entity.filter.getInstance(this));
    }
}
