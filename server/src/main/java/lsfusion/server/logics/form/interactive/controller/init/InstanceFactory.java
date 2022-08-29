package lsfusion.server.logics.form.interactive.controller.init;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.ContainerViewExtraType;
import lsfusion.server.logics.form.interactive.instance.design.BaseComponentViewInstance;
import lsfusion.server.logics.form.interactive.instance.design.ContainerViewInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.TreeGroupInstance;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.ActionOrPropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.function.Function;

public class InstanceFactory {

    public InstanceFactory() {
    }

    private final MAddExclMap<ObjectEntity, ObjectInstance> objectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<GroupObjectEntity, GroupObjectInstance> groupInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<TreeGroupEntity, TreeGroupInstance> treeInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<ActionOrPropertyObjectEntity, ActionOrPropertyObjectInstance> actionOrPropertyObjectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<PropertyDrawEntity, PropertyDrawInstance> propertyDrawInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<ContainerView, ContainerViewInstance> containerViewInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<ComponentView, BaseComponentViewInstance> baseComponentViewInstances = MapFact.mSmallStrongMap();


    public ObjectInstance getInstance(ObjectEntity entity) {
        ObjectInstance objectInstance = objectInstances.get(entity);
        if (objectInstance == null) {
            objectInstance = entity.baseClass.newInstance(entity);
            objectInstances.exclAdd(entity, objectInstance);
        }
        return objectInstance;
    }

    public GroupObjectInstance getInstance(GroupObjectEntity entity) {

        if (entity == null) {
            return null;
        }

        GroupObjectInstance groupInstance = groupInstances.get(entity);
        if (groupInstance == null) {
            ImOrderSet<ObjectInstance> objects = entity.getOrderObjects().mapOrderSetValues(this::getInstance);

            ImMap<ObjectInstance, PropertyObjectInstance> parentInstances = null;
            if(entity.isParent !=null) {
                parentInstances = entity.isParent.mapKeyValues(entity1 -> getInstance(entity1), entity2 -> getInstance(entity2));
            }

            groupInstance = new GroupObjectInstance(entity, objects, entity.propertyBackground != null ? getInstance(entity.propertyBackground) : null,
                    entity.propertyForeground != null ? getInstance(entity.propertyForeground) : null,
                    entity.propertyCustomOptions != null ? getInstance(entity.propertyCustomOptions) : null,
                    parentInstances, getInstance(entity.getProperties()));
            groupInstances.exclAdd(entity, groupInstance);
        }

        return groupInstance;
    }

    public TreeGroupInstance getInstance(TreeGroupEntity entity) {

        if (entity == null) {
            return null;
        }

        TreeGroupInstance treeInstance = treeInstances.get(entity);
        if (treeInstance == null) {

            // тут как бы с последействием, но "статичным"
            ImOrderSet<GroupObjectInstance> groups = entity.getGroups().mapOrderSetValues(this::getInstance);
            treeInstance = new TreeGroupInstance(entity, groups);
            treeInstances.exclAdd(entity, treeInstance);
        }
        return treeInstance;
    }

    public <P extends PropertyInterface> ImRevMap<P, ObjectInstance> getInstanceMap(ImRevMap<P, ObjectEntity> mapping) {
        return mapping.mapRevValues((ObjectEntity value) -> value.getInstance(InstanceFactory.this));
    }

    public <P extends PropertyInterface> PropertyObjectInstance<P> getInstance(PropertyObjectEntity<P> entity) {

        PropertyObjectInstance<P> propertyInstance = (PropertyObjectInstance<P>) actionOrPropertyObjectInstances.get(entity);
        if (propertyInstance == null) {
            propertyInstance = new PropertyObjectInstance<>(entity.property, getInstanceMap(entity.mapping));
            actionOrPropertyObjectInstances.exclAdd(entity, propertyInstance);
        }
        return propertyInstance;
    }

    private <P extends PropertyInterface> ImRevMap<P, ObjectInstance> getInstanceMap(PropertyRevImplement<P, ObjectEntity> entity) {
        return entity.mapping.mapRevValues((Function<ObjectEntity, ObjectInstance>) InstanceFactory.this::getInstance);
    }

    public <T, P extends PropertyInterface> ImMap<T, PropertyRevImplement<P, ObjectInstance>> getInstance(ImMap<T, PropertyRevImplement<P, ObjectEntity>> entities) {
        return entities.mapValues(entity -> new PropertyRevImplement<>(entity.property, getInstanceMap(entity)));
    }

        // временно
    public <P extends PropertyInterface> ActionOrPropertyObjectInstance<P, ?> getInstance(ActionOrPropertyObjectEntity<P, ?> entity) {
        if(entity instanceof PropertyObjectEntity)
            return getInstance((PropertyObjectEntity<P>)entity);
        else
            return getInstance((ActionObjectEntity<P>)entity);
    }

    public <P extends PropertyInterface> ActionObjectInstance<P> getInstance(ActionObjectEntity<P> entity) {

        ActionObjectInstance<P> actionInstance = (ActionObjectInstance<P>) actionOrPropertyObjectInstances.get(entity);
        if (actionInstance == null) {
            actionInstance = new ActionObjectInstance<>(entity.property, getInstanceMap(entity.mapping));
            actionOrPropertyObjectInstances.exclAdd(entity, actionInstance);
        }
        return actionInstance;
    }

    public PropertyDrawInstance getInstance(PropertyDrawEntity<? extends PropertyInterface> entity) {

        PropertyDrawInstance propertyDrawInstance = propertyDrawInstances.get(entity);
        if (propertyDrawInstance == null) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = entity.getColumnGroupObjects().mapOrderSetValues(this::getInstance);

            propertyDrawInstance = new PropertyDrawInstance<>(
                    entity,
                    getInstance(entity.getValueActionOrProperty()),
                    getInstance(entity.getDrawProperty()),
                    getInstance(entity.toDraw),
                    columnGroupObjects,
                    PropertyDrawExtraType.extras.mapValues((PropertyDrawExtraType type) -> entity.hasPropertyExtra(type) ? getInstance(entity.getPropertyExtra(type)) : null),
                    entity.lastAggrColumns.mapListValues((Function<PropertyObjectEntity, PropertyObjectInstance<?>>) this::getInstance)
            );
            propertyDrawInstances.exclAdd(entity, propertyDrawInstance);
        }

        return propertyDrawInstance;
    }

    public ContainerViewInstance getInstance(ContainerView entity) {
        ContainerViewInstance containerViewInstance = containerViewInstances.get(entity);
        if (containerViewInstance == null) {
            containerViewInstance = new ContainerViewInstance(
                    entity,
                    ContainerViewExtraType.extras.mapValues((ContainerViewExtraType type) -> {
                        PropertyObjectEntity<?> extra = entity.getExtra(type);
                        return extra != null ? getInstance(extra) : null;
                    })
            );
            containerViewInstances.exclAdd(entity, containerViewInstance);
        }
        return containerViewInstance;
    }

    public BaseComponentViewInstance getInstance(ComponentView entity) {
        BaseComponentViewInstance baseComponentViewInstance = baseComponentViewInstances.get(entity);
        if (baseComponentViewInstance == null) {
            baseComponentViewInstance = new BaseComponentViewInstance(entity);
            baseComponentViewInstances.exclAdd(entity, baseComponentViewInstance);
        }
        return baseComponentViewInstance;
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
