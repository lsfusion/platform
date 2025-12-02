package lsfusion.server.logics.form;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.util.HashMap;
import java.util.Map;

public class ObjectMapping {
    public Version version;

    public ObjectMapping(Version version) {
        this.version = version;
    }

    private Map<FormEntity, FormEntity> formEntityMap = new HashMap<>();
    private Map<ActionObjectEntity, ActionObjectEntity> actionObjectEntityMap = new HashMap<>();
    private Map<ObjectEntity, ObjectEntity> objectEntityMap = new HashMap<>();
    private Map<GroupObjectEntity, GroupObjectEntity> groupObjectEntityMap = new HashMap<>();
    private Map<PropertyDrawEntity, PropertyDrawEntity> propertyDrawEntityMap = new HashMap<>();
    private Map<PropertyObjectEntity, PropertyObjectEntity> propertyObjectEntityMap = new HashMap<>();
    private Map<FilterEntity, FilterEntity> filterEntityMap = new HashMap<>();
    private Map<RegularFilterEntity, RegularFilterEntity> regularFilterEntityMap = new HashMap<>();
    private Map<RegularFilterGroupEntity, RegularFilterGroupEntity> regularFilterGroupEntityMap = new HashMap<>();
    private Map<TreeGroupEntity, TreeGroupEntity> treeGroupEntityMap = new HashMap<>();

    private Map<FormView, FormView> formViewMap = new HashMap<>();
    private Map<ObjectView, ObjectView> objectViewMap = new HashMap<>();
    private Map<GroupObjectView, GroupObjectView> groupObjectViewMap = new HashMap<>();
    private Map<RegularFilterView, RegularFilterView> regularFilterViewMap = new HashMap<>();
    private Map<ComponentView, ComponentView> componentViewMap = new HashMap<>();

    public FormEntity get(FormEntity formEntity) {
        return get(formEntity, formEntity != null ? formEntity.getCanonicalName() : null);
    }

    public FormEntity get(FormEntity formEntity, String canonicalName) {
        FormEntity result = formEntityMap.get(formEntity);
        if (result == null && formEntity != null) {
            result = new FormEntity(formEntity, canonicalName, version);
            formEntityMap.put(formEntity, result);
            result.copy(formEntity, this);
        }
        return result;
    }

    public ActionObjectEntity get(ActionObjectEntity actionObjectEntity) {
        ActionObjectEntity result = actionObjectEntityMap.get(actionObjectEntity);
        if (result == null && actionObjectEntity != null) {
            result = new ActionObjectEntity(actionObjectEntity);
            actionObjectEntityMap.put(actionObjectEntity, result);
            result.copy(actionObjectEntity, this);
        }
        return result;
    }

    public ObjectEntity get(ObjectEntity objectEntity) {
        ObjectEntity result = objectEntityMap.get(objectEntity);
        if (result == null && objectEntity != null) {
            result = new ObjectEntity(objectEntity);
            objectEntityMap.put(objectEntity, result);
            result.copy(objectEntity, this);
        }
        return result;
    }

    public GroupObjectEntity get(GroupObjectEntity groupObjectEntity) {
        GroupObjectEntity result = groupObjectEntityMap.get(groupObjectEntity);
        if (result == null &&  groupObjectEntity != null) {
            result = new GroupObjectEntity(groupObjectEntity);
            groupObjectEntityMap.put(groupObjectEntity, result);
            result.copy(groupObjectEntity, this);
        }
        return result;
    }

    public PropertyDrawEntity get(PropertyDrawEntity propertyDrawEntity) {
        PropertyDrawEntity result = propertyDrawEntityMap.get(propertyDrawEntity);
        if (result == null && propertyDrawEntity != null) {
            result = new PropertyDrawEntity(propertyDrawEntity);
            propertyDrawEntityMap.put(propertyDrawEntity, result);
            result.copy(propertyDrawEntity, this);
        }
        return result;
    }

    public PropertyObjectEntity get(PropertyObjectEntity propertyObjectEntity) {
        PropertyObjectEntity result = propertyObjectEntityMap.get(propertyObjectEntity);
        if (result == null &&  propertyObjectEntity != null) {
            result = new PropertyObjectEntity(propertyObjectEntity);
            propertyObjectEntityMap.put(propertyObjectEntity, result);
            result.copy(propertyObjectEntity, this);
        }
        return result;
    }

    public FilterEntity get(FilterEntity filterEntity) {
        FilterEntity result = filterEntityMap.get(filterEntity);
        if (result == null && filterEntity != null) {
            result = new FilterEntity(filterEntity);
            filterEntityMap.put(filterEntity, result);
            result.copy(filterEntity, this);
        }
        return result;
    }

    public RegularFilterEntity get(RegularFilterEntity regularFilterEntity) {
        RegularFilterEntity result = regularFilterEntityMap.get(regularFilterEntity);
        if (result == null && regularFilterEntity != null) {
            result = new RegularFilterEntity(regularFilterEntity);
            regularFilterEntityMap.put(regularFilterEntity, result);
            result.copy(regularFilterEntity, this);
        }
        return result;
    }

    public RegularFilterGroupEntity get(RegularFilterGroupEntity regularFilterGroupEntity) {
        RegularFilterGroupEntity result = regularFilterGroupEntityMap.get(regularFilterGroupEntity);
        if (result == null &&  regularFilterGroupEntity != null) {
            result = new RegularFilterGroupEntity(regularFilterGroupEntity);
            regularFilterGroupEntityMap.put(regularFilterGroupEntity, result);
            result.copy(regularFilterGroupEntity, this);
        }
        return result;
    }

    public TreeGroupEntity get(TreeGroupEntity treeGroupEntity) {
        TreeGroupEntity result = treeGroupEntityMap.get(treeGroupEntity);
        if (result == null &&  treeGroupEntity != null) {
            result = new TreeGroupEntity(treeGroupEntity);
            treeGroupEntityMap.put(treeGroupEntity, result);
            result.copy(treeGroupEntity, this);
        }
        return result;
    }

    public FormView get(FormView formView) {
        FormView result = formViewMap.get(formView);
        if (result == null &&  formView != null) {
            result = new FormView(formView);
            formViewMap.put(formView, result);
            result.copy(formView, this);
        }
        return result;
    }

    public ObjectView get(ObjectView objectView) {
        ObjectView result = objectViewMap.get(objectView);
        if (result == null &&   objectView != null) {
            result = new ObjectView(objectView);
            objectViewMap.put(objectView, result);
            result.copy(objectView, this);
        }
        return result;
    }

    public GroupObjectView get(GroupObjectView groupObjectView) {
        GroupObjectView result = groupObjectViewMap.get(groupObjectView);
        if (result == null &&  groupObjectView != null) {
            result = new GroupObjectView(groupObjectView);
            groupObjectViewMap.put(groupObjectView, result);
            result.copy(groupObjectView, this);
            System.out.println("GroupObjectView copy finished");
        }
        return result;
    }

    public RegularFilterView get(RegularFilterView regularFilterView) {
        RegularFilterView result = regularFilterViewMap.get(regularFilterView);
        if (result == null &&   regularFilterView != null) {
            result = new RegularFilterView(regularFilterView);
            regularFilterViewMap.put(regularFilterView, result);
            result.copy(regularFilterView, this);
        }
        return result;
    }

    public <T extends ComponentView> T get(T componentView) {
        ComponentView result = componentViewMap.get(componentView);
        if (result == null && componentView != null) {
            if(componentView instanceof CalculationsView)
                result = new CalculationsView((CalculationsView) componentView);
            else if (componentView instanceof FilterControlsView)
                result = new FilterControlsView((FilterControlsView) componentView);
            else if (componentView instanceof FilterView)
                result = new FilterView((FilterView) componentView);
            else if (componentView instanceof GridView)
                result = new GridView((GridView) componentView);
            else if (componentView instanceof PropertyDrawView)
                result = new PropertyDrawView((PropertyDrawView) componentView);
            else if (componentView instanceof RegularFilterGroupView)
                result = new RegularFilterGroupView((RegularFilterGroupView) componentView);
            else if (componentView instanceof ToolbarView)
                result = new ToolbarView((ToolbarView) componentView);
            else if (componentView instanceof TreeGroupView)
                result = new TreeGroupView((TreeGroupView) componentView);
            else if (componentView instanceof ContainerView)
                result = new ContainerView((ContainerView) componentView);
            else result = new ComponentView(componentView);
            componentViewMap.put(componentView, result);
            if(componentView instanceof CalculationsView)
                ((CalculationsView) result).copy((CalculationsView) componentView, this);
            else if (componentView instanceof FilterControlsView)
                ((FilterControlsView) result).copy((FilterControlsView) componentView, this);
            else if (componentView instanceof FilterView)
                ((FilterView) result).copy((FilterView) componentView, this);
            else if (componentView instanceof GridView)
                ((GridView) result).copy((GridView) componentView, this);
            else if (componentView instanceof PropertyDrawView)
                ((PropertyDrawView) result).copy((PropertyDrawView) componentView, this);
            else if (componentView instanceof RegularFilterGroupView)
                ((RegularFilterGroupView) result).copy((RegularFilterGroupView) componentView, this);
            else if (componentView instanceof ToolbarView)
                ((ToolbarView) result).copy((ToolbarView) componentView, this);
            else if (componentView instanceof TreeGroupView)
                ((TreeGroupView) result).copy((TreeGroupView) componentView, this);
            else if (componentView instanceof ContainerView)
                ((ContainerView) result).copy((ContainerView) componentView, this);
            else result.copy(componentView, this);
        }
        return (T) result;
    }
}
