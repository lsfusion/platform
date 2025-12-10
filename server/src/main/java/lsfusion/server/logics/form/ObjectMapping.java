package lsfusion.server.logics.form;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PivotColumn;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntityOrPivotColumn;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public class ObjectMapping {
    public Version version;

    public ObjectMapping(Version version) {
        this.version = version;
    }

    //private Map<FormEntity, FormEntity> formEntityMap = new HashMap<>();
    private Map<ActionObjectEntity, ActionObjectEntity> actionObjectEntityMap = new HashMap<>();
    private Map<ObjectEntity, ObjectEntity> objectEntityMap = new HashMap<>();
    private Map<GroupObjectEntity, GroupObjectEntity> groupObjectEntityMap = new HashMap<>();
    private Map<PropertyDrawEntity, PropertyDrawEntity> propertyDrawEntityMap = new HashMap<>();
    private Map<PropertyObjectEntity, PropertyObjectEntity> propertyObjectEntityMap = new HashMap<>();
    private Map<FilterEntity, FilterEntity> filterEntityMap = new HashMap<>();
    private Map<RegularFilterEntity, RegularFilterEntity> regularFilterEntityMap = new HashMap<>();
    private Map<RegularFilterGroupEntity, RegularFilterGroupEntity> regularFilterGroupEntityMap = new HashMap<>();
    private Map<TreeGroupEntity, TreeGroupEntity> treeGroupEntityMap = new HashMap<>();

    //private Map<FormView, FormView> formViewMap = new HashMap<>();
    private Map<ObjectView, ObjectView> objectViewMap = new HashMap<>();
    private Map<GroupObjectView, GroupObjectView> groupObjectViewMap = new HashMap<>();
    private Map<RegularFilterView, RegularFilterView> regularFilterViewMap = new HashMap<>();
    private Map<ComponentView, ComponentView> componentViewMap = new HashMap<>();

/*    public FormEntity get(FormEntity key) {
        FormEntity result = formEntityMap.get(key);
        if (result == null && key != null) {
            result = new FormEntity(null, null, null, null, false, version);
            key.copy(result, this);
            formEntityMap.put(key, result);
        }
        return result;
    }*/

    public void put(ActionObjectEntity key, ActionObjectEntity value) {
        actionObjectEntityMap.put(key, value);
    }
    public synchronized ActionObjectEntity get(ActionObjectEntity actionObjectEntity) {
        ActionObjectEntity result = actionObjectEntityMap.get(actionObjectEntity);
        if (result == null) {
            result = new ActionObjectEntity(actionObjectEntity, this);
        }
        return result;
    }

    public void put(ObjectEntity key, ObjectEntity value) {
        objectEntityMap.put(key, value);
    }
    public ObjectEntity getFinal(ObjectEntity objectEntity) {
        return objectEntityMap.get(objectEntity);
    }
    public synchronized ObjectEntity get(ObjectEntity objectEntity) {
        ObjectEntity result = objectEntityMap.get(objectEntity);
        if (result == null) {
            result = new ObjectEntity(objectEntity, this);
        }
        return result;
    }

    public void put(GroupObjectEntity key, GroupObjectEntity value) {
        groupObjectEntityMap.put(key, value);
    }
    public GroupObjectEntity getFinal(GroupObjectEntity groupObjectEntity) {
        return groupObjectEntityMap.get(groupObjectEntity);
    }
    public synchronized GroupObjectEntity get(GroupObjectEntity groupObjectEntity) {
        GroupObjectEntity result = groupObjectEntityMap.get(groupObjectEntity);
        if (result == null) {
            result = new GroupObjectEntity(groupObjectEntity,  this);
        }
        return result;
    }

    public void put(PropertyDrawEntity key, PropertyDrawEntity value) {
        propertyDrawEntityMap.put(key, value);
    }
    public PropertyDrawEntity getFinal(PropertyDrawEntity propertyDrawEntity) {
        return propertyDrawEntityMap.get(propertyDrawEntity);
    }
    public synchronized PropertyDrawEntity get(PropertyDrawEntity propertyDrawEntity) {
        PropertyDrawEntity result = propertyDrawEntityMap.get(propertyDrawEntity);
        if (result == null) {
            result = new PropertyDrawEntity(propertyDrawEntity,  this);
        }
        return result;
    }

    public void put(PropertyObjectEntity key, PropertyObjectEntity value) {
        propertyObjectEntityMap.put(key, value);
    }
    public synchronized PropertyObjectEntity get(PropertyObjectEntity propertyObjectEntity) {
        if(propertyObjectEntity == null)
            return null;

        PropertyObjectEntity result = propertyObjectEntityMap.get(propertyObjectEntity);
        if (result == null) {
            result = new PropertyObjectEntity(propertyObjectEntity, this);
        }
        return result;
    }

    public void put(FilterEntity key, FilterEntity value) {
        filterEntityMap.put(key, value);
    }
    public synchronized FilterEntity get(FilterEntity filterEntity) {
        FilterEntity result = filterEntityMap.get(filterEntity);
        if (result == null) {
            result = new FilterEntity(filterEntity, this);
        }
        return result;
    }

    public void put(RegularFilterEntity key, RegularFilterEntity value) {
        regularFilterEntityMap.put(key, value);
    }
    public synchronized RegularFilterEntity get(RegularFilterEntity regularFilterEntity) {
        RegularFilterEntity result = regularFilterEntityMap.get(regularFilterEntity);
        if (result == null) {
            result = new RegularFilterEntity(regularFilterEntity, this);
        }
        return result;
    }

    public void put(RegularFilterGroupEntity key, RegularFilterGroupEntity value) {
        regularFilterGroupEntityMap.put(key, value);
    }
    public RegularFilterGroupEntity getFinal(RegularFilterGroupEntity regularFilterGroupEntity) {
        return regularFilterGroupEntityMap.get(regularFilterGroupEntity);
    }
    public synchronized RegularFilterGroupEntity get(RegularFilterGroupEntity regularFilterGroupEntity) {
        RegularFilterGroupEntity result = regularFilterGroupEntityMap.get(regularFilterGroupEntity);
        if (result == null) {
            result = new RegularFilterGroupEntity(regularFilterGroupEntity, this);
        }
        return result;
    }

    public void put(TreeGroupEntity key, TreeGroupEntity value) {
        treeGroupEntityMap.put(key, value);
    }
    public synchronized TreeGroupEntity get(TreeGroupEntity treeGroupEntity) {
        TreeGroupEntity result = treeGroupEntityMap.get(treeGroupEntity);
        if (result == null) {
            result = new TreeGroupEntity(treeGroupEntity, this);
        }
        return result;
    }

    public OrderEntity get(OrderEntity key) {
        return key instanceof ObjectEntity ? get((ObjectEntity) key) : get((PropertyObjectEntity) key);
    }

    public ActionOrPropertyObjectEntity get(ActionOrPropertyObjectEntity key) {
        return key instanceof ActionObjectEntity ?  get((ActionObjectEntity) key) : get((PropertyObjectEntity) key);
    }

    public PropertyDrawEntityOrPivotColumn get(PropertyDrawEntityOrPivotColumn key) {
        return key instanceof PropertyDrawEntity ? get((PropertyDrawEntity) key) : get((PivotColumn) key);
    }

    public PivotColumn get(PivotColumn pivotColumn) {
        return new PivotColumn(get(pivotColumn.groupObject));
    }
    public FormEntity.EventAction get(FormEntity.EventAction eventAction) {
        return new FormEntity.EventAction(eventAction, this);
    }

/*    public void put(FormView key, FormView value) {
        formViewMap.put(key, value);
    }
    public FormView get(FormView formView) {
        FormView result = formViewMap.get(formView);
        if (result == null &&  formView != null) {
            result = new FormView(formView, this);
        }
        return result;
    }*/

    public void put(ObjectView key, ObjectView value) {
        objectViewMap.put(key, value);
    }
    public synchronized ObjectView get(ObjectView objectView) {
        ObjectView result = objectViewMap.get(objectView);
        if (result == null) {
            result = new ObjectView(objectView, this);
        }
        return result;
    }

    public void put(GroupObjectView key, GroupObjectView value) {
        groupObjectViewMap.put(key, value);
    }
    public synchronized GroupObjectView get(GroupObjectView groupObjectView) {
        GroupObjectView result = groupObjectViewMap.get(groupObjectView);
        if (result == null) {
            result = new GroupObjectView(groupObjectView, this);
        }
        return result;
    }

    public void put(RegularFilterView key, RegularFilterView value) {
        regularFilterViewMap.put(key, value);
    }
    public synchronized RegularFilterView get(RegularFilterView regularFilterView) {
        RegularFilterView result = regularFilterViewMap.get(regularFilterView);
        if (result == null) {
            result = new RegularFilterView(regularFilterView, this);
        }
        return result;
    }

    public void put(ComponentView key, ComponentView value) {
        componentViewMap.put(key, value);
    }
    public <T extends ComponentView> T getFinal(T componentView) {
        return (T) componentViewMap.get(componentView);
    }
    public synchronized <T extends ComponentView> T get(T componentView) {
        if(componentView == null)
            return null;

        ComponentView result = componentViewMap.get(componentView);
        if (result == null) {
            if(componentView instanceof CalculationsView)
                result = new CalculationsView((CalculationsView) componentView, this);
            else if (componentView instanceof FilterControlsView)
                result = new FilterControlsView((FilterControlsView) componentView, this);
            else if (componentView instanceof FilterView)
                result = new FilterView((FilterView) componentView, this);
            else if (componentView instanceof GridView)
                result = new GridView((GridView) componentView, this);
            else if (componentView instanceof PropertyDrawView)
                result = new PropertyDrawView((PropertyDrawView) componentView, this);
            else if (componentView instanceof RegularFilterGroupView)
                result = new RegularFilterGroupView((RegularFilterGroupView) componentView, this);
            else if (componentView instanceof ToolbarView)
                result = new ToolbarView((ToolbarView) componentView, this);
            else if (componentView instanceof TreeGroupView)
                result = new TreeGroupView((TreeGroupView) componentView, this);
            else if (componentView instanceof ContainerView)
                result = new ContainerView((ContainerView) componentView, this);
            else
                result = new ComponentView(componentView, this);
            componentViewMap.put(componentView, result);
        }
        return (T) result;
    }

    public PropertyGroupContainerView get(PropertyGroupContainerView key) {
        return key instanceof GroupObjectView ? get((GroupObjectView) key) : get((TreeGroupView) key);
    }
    public PropertyDrawViewOrPivotColumn get(PropertyDrawViewOrPivotColumn key) {
        return key instanceof PropertyDrawView ? get((PropertyDrawView) key) : get((PivotColumn) key);
    }
    public TreeGroupView get(TreeGroupView key) {
        return (TreeGroupView) get((ComponentView) key);
    }
    public PropertyDrawView get(PropertyDrawView key) {
        return (PropertyDrawView) get((ComponentView) key);
    }

    public ActionObjectSelector get(ActionObjectSelector key) {
        return key instanceof ActionObjectEntity ? get((ActionObjectEntity) key) : (context) -> {
            ActionObjectEntity<?> action = key.getAction(context);
            return action != null ? get(action) : null;
        }; // first check is basically optimization
    }

    public <T extends PropertyInterface> PropertyRevImplement<T, ObjectEntity> get(PropertyRevImplement<T, ObjectEntity> prop) {
        return new PropertyRevImplement<>(prop.property, prop.mapping.mapRevValues((ObjectEntity obj) -> get(obj)));
    }
    public FormEntity.ExProperty get(FormEntity.ExProperty key) {
        return new FormEntity.ExProperty(key, this);
    }
    public GroupObjectEntity.ExGroupProperty get(GroupObjectEntity.ExGroupProperty key) {
        return new GroupObjectEntity.ExGroupProperty(key, this);
    }
    public GridView.ExContainerView get(GridView.ExContainerView key) {
        return new GridView.ExContainerView(key, this);
    }
}
