package platform.server.logics.scripted;

import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.LsfLogicsParser;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.view.*;
import platform.server.form.view.panellocation.PanelLocationView;
import platform.server.form.view.panellocation.ShortcutPanelLocationView;
import platform.server.form.view.panellocation.ToolbarPanelLocationView;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.scripted.proxy.ViewProxyUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.server.logics.scripted.ScriptingLogicsModule.InsertPosition.IN;

public class ScriptingFormView extends DefaultFormView {

    private final ScriptingLogicsModule LM;
    private final ScriptingErrorLog errLog;
    private final LsfLogicsParser parser;

    private final Map<String, ComponentView> sidToComponent = new HashMap<String, ComponentView>();

    public ScriptingFormView(FormEntity entity, boolean applyDefault, ScriptingLogicsModule iLM) {
        super(entity, applyDefault);

        this.LM = iLM;
        this.errLog = LM.getErrLog();
        this.parser = LM.getParser();

        mainContainer.setSID(getMainContainerSID());
        addComponentToMapping(mainContainer);

        for (TreeGroupView tree : mtreeGroups.values()) {
            tree.setSID(getTreeSID(tree.entity));
            addComponentToMapping(tree);
        }

        for (RegularFilterGroupView filterGroup : regularFilters) {
            filterGroup.setSID(getRegularFilterGroupSID(filterGroup.entity));
            addComponentToMapping(filterGroup);
        }

        for (GroupObjectView group : mgroupObjects.values()) {
            group.grid.setSID(getGridSID(group.entity));
            group.showType.setSID(getShowTypeSID(group.entity));
            addComponentToMapping(group.grid);
            addComponentToMapping(group.showType);
        }

        for (ObjectView obj : mobjects.values()) {
            obj.classChooser.setSID(getClassChooserSID(obj.entity));
            addComponentToMapping(obj.classChooser);
        }

        setupFormButton(printButton, "print");
        setupFormButton(editButton, "edit");
        setupFormButton(xlsButton, "xls");
        setupFormButton(nullButton, "null");
        setupFormButton(refreshButton, "refresh");
        setupFormButton(applyButton, "apply");
        setupFormButton(cancelButton, "cancel");
        setupFormButton(okButton, "ok");
        setupFormButton(closeButton, "close");

        if (applyDefault) {
            formButtonContainer.setSID(getFormButtonContainerSID());

            for (Map.Entry<TreeGroupView, ContainerView> entry : treeContainers.entrySet()) {
                entry.getValue().setSID(getTreeContainerSID(entry.getKey().entity));
            }

            for (Map.Entry<GroupObjectView, Map<AbstractGroup, ContainerView>> entry : groupPropertyContainers.entrySet()) {
                for (Map.Entry<AbstractGroup, ContainerView> groupContainer : entry.getValue().entrySet()) {
                    groupContainer.getValue().setSID(getPropertyGroupContainerSID(LM, entry.getKey().entity, groupContainer.getKey()));
                }
            }

            for (GroupObjectView groupObject : groupObjects) {
                setGroupObjectContainerSID(groupObject);
            }

            fillSIDMapping(mainContainer);
        }
    }

    private void setGroupObjectContainerSID(GroupObjectView groupObject) {
        setContainerSID(groupContainers.get(groupObject),getGroupObjectContainerSID(groupObject.entity));
        setContainerSID(panelContainers.get(groupObject),getPanelContainerSID(groupObject.entity));
        setContainerSID(gridContainers.get(groupObject),getGridContainerSID(groupObject.entity));
        setContainerSID(controlsContainers.get(groupObject),getControlsContainerSID(groupObject.entity));
        setContainerSID(filterContainers.get(groupObject),getFilterContainerSID(groupObject.entity));
    }

    // todo : тут вообще всю логику надо перефигачить под инкрементную модель (чтобы по ходу могли добавляться свойства и groupObject)
    private void setContainerSID(ContainerView container, String sID) {
        container.setSID(sID);
        addComponentToMapping(container);
    }

    @Override
    public GroupObjectView addGroupObjectEntity(GroupObjectEntity groupObject) {
        GroupObjectView view = super.addGroupObjectEntity(groupObject);
        setGroupObjectContainerSID(view);
        return view;
    }

    private void setupFormButton(PropertyDrawView function, String type) {
        setClientFunctionSID(function, type);
        addComponentToMapping(function);
    }

    private void fillSIDMapping(ComponentView component) {
        assert component.getSID() != null;

        addComponentToMapping(component);

        if (component instanceof ContainerView) {
            for (ComponentView child : ((ContainerView) component).getChildren()) {
                fillSIDMapping(child);
            }
        }
    }

    private void addComponentToMapping(ComponentView component) {
        sidToComponent.put(component.getSID(), component);
    }

    private static String getMainContainerSID() {
        return "main";
    }

    private static String getTreeContainerSID(TreeGroupEntity entity) {
        return entity.getSID() + ".box";
    }

    private static String getTreeSID(TreeGroupEntity entity) {
        return entity.getSID() + ".tree";
    }

    private static String getGroupObjectContainerSID(GroupObjectEntity entity) {
        return entity.getSID() + ".box";
    }

    private static String getPanelContainerSID(GroupObjectEntity entity) {
        return entity.getSID() + ".panel";
    }

    private static String getGridSID(GroupObjectEntity entity) {
        return entity.getSID() + ".grid";
    }

    private static String getGridContainerSID(GroupObjectEntity entity) {
        return entity.getSID() + ".grid.box";
    }

    private static String getShowTypeSID(GroupObjectEntity entity) {
        return entity.getSID() + ".showType";
    }

    private static String getClassChooserSID(ObjectEntity entity) {
        return entity.getSID() + ".classChooser";
    }

    private static String getControlsContainerSID(GroupObjectEntity entity) {
        return entity.getSID() + ".controls";
    }

    private static String getPropertyGroupContainerSID(ScriptingLogicsModule lm, GroupObjectEntity group, AbstractGroup propertyGroup) {
        String propertyGroupSID = propertyGroup.getSID();
        if (propertyGroupSID.contains("_")) {
            String[] sids = propertyGroupSID.split("_", 2);
            propertyGroupSID = sids[1];
        }
        // todo : здесь конечно совсем хак - нужно более четку схему сделать
//        if (lm.getGroupBySID(propertyGroupSID) != null) {
//            используем простое имя для групп данного модуля
//            propertyGroupSID = lm.transformSIDToName(propertyGroupSID);
//        }
        return group.getSID() + "." + propertyGroupSID;
    }

    private static String getRegularFilterGroupSID(RegularFilterGroupEntity entity) {
        return "filters." + entity.getSID();
    }

    private static String getFilterContainerSID(GroupObjectEntity entity) {
        return entity.getSID() + ".filters";
    }

    private static String getFormButtonContainerSID() {
        return "functions.box";
    }

    private static void setClientFunctionSID(PropertyDrawView function, String type) {
        function.setSID("functions." + type);
    }

    public ComponentView getComponentBySID(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return getComponentBySID(sid, true);
    }

    public ComponentView getComponentBySID(String sid, boolean hasToExist) throws ScriptingErrorLog.SemanticErrorException {
        ComponentView component = sidToComponent.get(sid);
        if (hasToExist && component == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return component;
    }

    public ContainerView getParentContainer(ComponentView child) throws ScriptingErrorLog.SemanticErrorException {
        if (child == null) {
            errLog.emitComponentIsNullError(parser, "can't get parent:");
        }

        return child.getContainer();
    }

    public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ViewProxyUtil.setObjectProperty(propertyReceiver, propertyName, propertyValue);
        } catch (Exception e) {
            errLog.emitUnableToSetPropertyError(parser, propertyName, e.getMessage());
        }
    }

    public void addIntersection(ComponentView comp1, DoNotIntersectSimplexConstraint cons, ComponentView comp2) throws ScriptingErrorLog.SemanticErrorException {
        if (comp1 == null || comp2 == null) {
            errLog.emitComponentIsNullError(parser, "can't add intersection:");
        }

        if (comp1.getContainer() != comp2.getContainer()) {
            errLog.emitIntersectionInDifferentContainersError(parser);
        }

        super.addIntersection(comp2, comp1, cons);
    }

    public PanelLocationView createPanelLocation(boolean toolbar, PropertyDrawView property, boolean defaultProperty) {
        PanelLocationView panelLocation;
        if (toolbar) {
            panelLocation = new ToolbarPanelLocationView();
        } else {
            panelLocation = new ShortcutPanelLocationView(property, defaultProperty);
        }
        return panelLocation;
    }

    public ContainerView createNewComponent(String sid, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent) throws ScriptingErrorLog.SemanticErrorException {
        assert anchorComponent != null && sid != null && sid.matches("[a-zA-Z][a-zA-Z_0-9]*(\\.[a-zA-Z][a-zA-Z_0-9]*)*");

        if (getComponentBySID(sid, false) != null) {
            errLog.emitAlreadyDefinedError(parser, "component", sid);
        }

        ContainerView container = new ContainerView(idGenerator.idShift());
        container.setSID(sid);

        sidToComponent.put(sid, container);

        moveComponent(container, pos, anchorComponent);

        return container;
    }

    public void moveComponent(ComponentView component, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null && anchorComponent != null;

        ContainerView parent = null;
        if (pos == IN) {
            if (!(anchorComponent instanceof ContainerView)) {
                errLog.emitComponentMustBeAContainerError(parser);
            }
            parent = (ContainerView) anchorComponent;
        } else {
            if (anchorComponent == mainContainer) {
                errLog.emitInsertBeforeAfterMainContainerError(parser);
            }
            parent = anchorComponent.getContainer();
        }

        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            if (container.isAncestorOf(parent)) {
                errLog.emitIllegalMoveComponentToSubcomponent(parser, container.getSID(), parent.getSID());
            }
        }

        switch (pos) {
            case IN:
                parent.add(component);
                break;
            case BEFORE:
                parent.addBefore(component, anchorComponent);
                break;
            case AFTER:
                parent.addAfter(component, anchorComponent);
                break;
        }
    }

    public void removeComponent(ComponentView component, boolean cascade) throws ScriptingErrorLog.SemanticErrorException {
        if (component == null) {
            errLog.emitComponentIsNullError(parser, "can't remove component:");
        }

        if (component == mainContainer) {
            errLog.emitRemoveMainContainerError(parser);
        }

        component.getContainer().remove(component);

        //не удаляем компоненты (не-контейнеры) из пула, чтобы можно было опять их использовать в настройке
        if (component instanceof ContainerView) {
            sidToComponent.remove(component.getSID());
            if (cascade) {
                for (ComponentView child : ((ContainerView) component).getChildren()) {
                    removeComponent(child, true);
                }
            }
        }
    }

    public GroupObjectView getGroupObject(String sid) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObjectEntity = entity.getGroupObject(sid);
        if (groupObjectEntity == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return getGroupObject(groupObjectEntity);
    }

    public PropertyDrawView getPropertyView(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = entity.getPropertyDraw(name);
        if (drawEntity == null) {
            drawEntity = entity.getPropertyDraw(LM.findLPByCompoundName(name));
        }

        return get(drawEntity);
    }

    public PropertyDrawView getPropertyView(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty mappedProp = LM.getPropertyWithMapping(entity, name, mapping);
        return get(entity.getPropertyDraw(mappedProp.property, mappedProp.mapping));
    }
}
