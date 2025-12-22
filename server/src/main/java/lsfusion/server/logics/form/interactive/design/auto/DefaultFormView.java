package lsfusion.server.logics.form.interactive.design.auto;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormContainerSet;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyContainersView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainersView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.admin.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static lsfusion.server.logics.form.interactive.design.FormContainerSet.getFormSID;

public class DefaultFormView extends FormView<DefaultFormView> implements PropertyContainersView<DefaultFormView> {

    public static int GROUP_CONTAINER_LINES_COUNT = 3;

    private PropertyContainersView getPropertyGroupContainer(GroupObjectEntity groupObject) {
        if(groupObject == null)
            return this;
        if (groupObject.isInTree())
            return get(groupObject.treeGroup);
        return get(groupObject).grid;
    }

    public static class ContainerSet implements MappingInterface<ContainerSet> {
        public final ContainerView box;
        public final ContainerView panel;
        public final ContainerView group;
        public final ContainerView toolbarBox;
        public final ContainerView toolbar;
        public final ContainerView popup;
        public final ContainerView toolbarLeft;
        public final ContainerView toolbarRight;

        public final ContainerView filterBox;
        public final ContainerView filterGroups;
        public final ContainerView filters;

        // the main idea that it should be global so all the modules would see it to use the same container for the same property groups
        public final Function<Group, ContainerView> groupProps;

        public ContainerSet(DefaultFormView form, PropertyContainersView groupView, ContainerView<?> box, ContainerView<?> panel, ContainerView<?> group, ContainerView<?> toolbarBox, ContainerView<?> toolbar, ContainerView<?> popup, ContainerView<?> toolbarLeft, ContainerView<?> toolbarRight, ContainerView<?> filterBox, ContainerView<?> filterGroups, ContainerView<?> filters, Version version) {
            this(box, panel, group, toolbarBox, toolbar, popup, toolbarLeft, toolbarRight, filterBox, filterGroups, filters, form.getGroupProps(panel, groupView));

            Function<PropertyContainersView, ContainerSet> containerGetter = PropertyContainersView::getContainers;
            box.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).box);
            panel.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).panel);
            group.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).group);
            toolbarBox.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).toolbarBox);
            toolbar.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).toolbar);
            popup.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).popup);
            toolbarLeft.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).toolbarLeft);
            toolbarRight.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).toolbarRight);

            if(filterBox != null) {
                filterBox.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).filterBox);
                filterGroups.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).filterGroups);
                filters.setAddParentPC(groupView, pcv -> containerGetter.apply(pcv).filters);
            }

            form.addComponentsToMapping(this, version);
        }
        public ContainerSet(ContainerView box, ContainerView panel, ContainerView group, ContainerView toolbarBox, ContainerView toolbar, ContainerView popup, ContainerView toolbarLeft, ContainerView toolbarRight, ContainerView filterBox, ContainerView filterGroups, ContainerView filters, Function<Group, ContainerView> groupProps) {
            this.box = box;
            this.panel = panel;
            this.group = group;
            this.toolbarBox = toolbarBox;
            this.toolbar = toolbar;
            this.popup = popup;
            this.toolbarLeft = toolbarLeft;
            this.toolbarRight = toolbarRight;

            this.filterBox = filterBox;
            this.filters = filters;
            this.filterGroups = filterGroups;

            this.groupProps = groupProps;
        }

        public ContainerView[] getContainers() { // filters are nullable
            return new ContainerView[] {box, panel, group, toolbarBox, toolbar, popup, toolbarLeft, toolbarRight, filterBox, filters, filterGroups};
        }

        public ContainerSet(ContainerSet containers, ObjectMapping mapping) {
            this(mapping.get(containers.box), mapping.get(containers.panel), mapping.get(containers.group), mapping.get(containers.toolbarBox), mapping.get(containers.toolbar), mapping.get(containers.popup), mapping.get(containers.toolbarLeft), mapping.get(containers.toolbarRight), mapping.get(containers.filterBox), mapping.get(containers.filterGroups), mapping.get(containers.filters),
                    group -> mapping.get(containers.groupProps.apply(group)));
        }

        @Override
        public ContainerSet get(ObjectMapping mapping) {
            return new ContainerSet(this, mapping);
        }
    }

    @Override
    public ContainerSet getContainers() {
        return containers;
    }

    public ContainerView getBoxContainer(PropertyContainersView groupObject) { return groupObject.getContainers().box; }
    public ContainerView getBoxContainer(GroupObjectEntity groupObject) { return getBoxContainer(getPropertyGroupContainer(groupObject)); }

    public ContainerView getToolbarBoxContainer(PropertyContainersView groupObject) { return groupObject.getContainers().toolbarBox; }

    public ContainerView getToolbarBoxContainer(GroupObjectEntity groupObject) { return getToolbarBoxContainer(get(groupObject).grid); }
    public ContainerView getToolbarLeftContainer(PropertyContainersView groupObject) { return groupObject.getContainers().toolbarLeft; }
    public ContainerView getToolbarRightContainer(PropertyContainersView groupObject) { return groupObject.getContainers().toolbarRight; }
    public ContainerView getPopupContainer(PropertyContainersView groupObject) { return groupObject.getContainers().popup; }

    public ContainerView getFilterGroupsContainer(GroupObjectEntity groupObject) {
        return getPropertyGroupContainer(groupObject).getContainers().filterGroups; }

    public ContainerSet containers;

    public DefaultFormView(FormEntity formEntity, Version version) {
        super(formEntity, version);

        FormContainerSet formSet = FormContainerSet.fillContainers(mainContainer, containerFactory, version);
        containers = formSet.getContainerSet(this, version);
    }

    public static String getToolbarBoxContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARBOX_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarRightContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARRIGHT_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarLeftContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARLEFT_CONTAINER + "(" + goName + ")";
    }

    public static String getFilterGroupsContainerSID(String goName) {
        return GroupObjectContainerSet.FILTERGROUPS_CONTAINER + "(" + goName + ")";
    }

    public static String getBoxContainerSID(String goName) {
        return GroupObjectContainerSet.BOX_CONTAINER + "(" + goName + ")";
    }

    public static String getFilterBoxContainerSID(String goName) {
        return GroupObjectContainerSet.FILTERBOX_CONTAINER + "(" + goName + ")";
    }

    public static String getPanelContainerSID(String goName) {
        return GroupObjectContainerSet.PANEL_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBAR_CONTAINER + "(" + goName + ")";
    }

    public static String getPopupContainerSID(String goName) {
        return GroupObjectContainerSet.POPUP_CONTAINER + "(" + goName + ")";
    }

    public static String getGOGroupContainerSID(String goName) {
        return GroupObjectContainerSet.GROUP_CONTAINER + "(" + goName + ")";
    }

    public static String getGroupContainerSID(String pgName) {
        return FormContainerSet.GROUP_CONTAINER + "(" + pgName + ")";
    }

    public static String getObjectsContainerSID() {
        return FormContainerSet.OBJECTS_CONTAINER;
    }

    public static String getToolbarBoxContainerSID() {
        return FormContainerSet.TOOLBARBOX_CONTAINER;
    }

    public static String getToolbarLeftContainerSID() {
        return FormContainerSet.TOOLBARLEFT_CONTAINER;
    }

    public static String getToolbarRightContainerSID() {
        return FormContainerSet.TOOLBARRIGHT_CONTAINER;
    }

    public static String getPanelContainerSID() {
        return FormContainerSet.PANEL_CONTAINER;
    }

    public static String getToolbarContainerSID() {
        return FormContainerSet.TOOLBAR_CONTAINER;
    }

    public static String getPopupContainerSID() {
        return FormContainerSet.POPUP_CONTAINER;
    }

    public void initDefaultProps(Version version) {
        PropertyDrawView editFunction = get(entity.editActionPropertyDraw);
        setupFormButton(editFunction, version);

        PropertyDrawView dropFunction = get(entity.dropActionPropertyDraw);
        setupFormButton(dropFunction, version);

        PropertyDrawView refreshFunction = get(entity.refreshActionPropertyDraw);
        setupFormButton(refreshFunction, version);
        refreshFunction.setDrawAsync(true, version);

        PropertyDrawView applyFunction = get(entity.applyActionPropertyDraw);
        setupFormButton(applyFunction, version);

        PropertyDrawView cancelFunction = get(entity.cancelActionPropertyDraw);
        setupFormButton(cancelFunction, version);

        PropertyDrawView okFunction = get(entity.okActionPropertyDraw);
        setupFormButton(okFunction, version);

        PropertyDrawView closeFunction = get(entity.closeActionPropertyDraw);
        setupFormButton(closeFunction, version);

        PropertyDrawView shareFunction = null;
        if(entity.shareActionPropertyDraw != null) {
            shareFunction = get(entity.shareActionPropertyDraw);
            setupFormButton(shareFunction, version);
        }

        PropertyDrawView customizeFunction = null;
        if(entity.customizeActionPropertyDraw != null) {
            customizeFunction = get(entity.customizeActionPropertyDraw);
            setupFormButton(customizeFunction, version);
        }

        PropertyDrawView logMessage = get(entity.logMessagePropertyDraw);

        ContainerView toolbarLeftContainer = getToolbarLeftContainer(this);
        ContainerView toolbarRightContainer = getToolbarRightContainer(this);
        ContainerView popupContainer = getPopupContainer(this);

        toolbarLeftContainer.add(logMessage, version); // otherwise it will go to OBJECTS container which has types COLUMNS and this type doesnt respect SHOWIF

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();
        if (toolbarTopLeft) {
            toolbarRightContainer.add(okFunction, version);
            toolbarRightContainer.add(closeFunction, version);
        } else {
            toolbarRightContainer.add(refreshFunction, version);
        }
        toolbarRightContainer.add(dropFunction, version);
        toolbarRightContainer.add(applyFunction, version);
        toolbarRightContainer.add(cancelFunction, version);
        if (toolbarTopLeft) {
            toolbarRightContainer.add(refreshFunction, version);
        } else {
            toolbarRightContainer.add(okFunction, version);
            toolbarRightContainer.add(closeFunction, version);
        }

        popupContainer.add(editFunction, version);
        if(shareFunction != null)
            toolbarLeftContainer.add(shareFunction, version);
        if(customizeFunction != null)
            toolbarLeftContainer.add(customizeFunction, version);
    }

    private void setupFormButton(PropertyDrawView action, Version version) {
        action.setShowChangeKey(false, version);
        action.setShowChangeMouse(false, version);
        action.setFocusable(false, version);
        action.entity.setEditType(PropertyEditType.EDITABLE, version);
        action.setAlignment(FlexAlignment.STRETCH, version);
    }

    private void addToPropertyContainer(PropertyDrawView property, Version version) {
        getPropertyContainer(property.entity, version).addDefault(property, version); // assert new
    }

    private void moveInPropertyContainer(PropertyDrawView property, ComplexLocation<PropertyDrawView> location, Version version) {
        getPropertyContainer(property.entity, version).addOrMoveDefault(property, location, version);
    }

    @Override
    public void updatePropertyDrawContainer(PropertyDrawView property, Version version) {
        getPropertyContainer(property.entity, version).addOrMoveDefault(property, null, version);
    }

    private void addToObjectsContainer(ContainerView boxContainer, Version version) {
        getBoxContainer(this).addDefault(boxContainer, version); // assert new
    }

    private void moveInObjectsContainer(ContainerView boxContainer, ComplexLocation<GroupObjectEntity> location, Version version) {
        getBoxContainer(this).addOrMoveDefault(boxContainer, location.<ComponentView>map(this::getBoxContainer), version);
    }

    private void addComponentsToMapping(ContainerSet containers, Version version) {
        for(ContainerView container : containers.getContainers())
            if(container != null)
                addComponentToMapping(container, version);
    }

    // добавление в панель по сути, так как добавление в grid происходит уже на живой форме

    @Override
    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, Version version) {
        GroupObjectView view = super.addGroupObject(groupObject, version);

        if(!view.entity.isInTree()) {
            GridView grid = view.grid;

            GroupObjectContainerSet groupSet = GroupObjectContainerSet.create(grid, containerFactory, version);
            grid.containers = groupSet.getContainerSet(this, grid, version);

            addToObjectsContainer(groupSet.getBoxContainer(), version);
        }

        return view;
    }

    @Override
    public void moveGroupObject(GroupObjectView groupObject, ComplexLocation<GroupObjectEntity> location, Version version) {
        super.moveGroupObject(groupObject, location, version);

        // assert !isInTree
        moveInObjectsContainer(getBoxContainer(groupObject.entity), location, version);
    }

    @Override
    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, Version version) {
        TreeGroupView view = super.addTreeGroup(treeGroup, version);

        TreeGroupContainerSet treeSet = TreeGroupContainerSet.create(view, containerFactory, version);
        view.containers = treeSet.getContainerSet(this, view, version);

        addToObjectsContainer(treeSet.getBoxContainer(), version);
        return view;
    }

    @Override
    public void moveTreeGroup(TreeGroupView treeGroup, ComplexLocation<GroupObjectEntity> location, Version version) {
        super.moveTreeGroup(treeGroup, location, version);

        moveInObjectsContainer(getBoxContainer(treeGroup), location, version);
    }

    @Override
    public PropertyDrawView addPropertyDraw(PropertyDrawEntity propertyDraw, Version version) {
        PropertyDrawView view = super.addPropertyDraw(propertyDraw, version);

        addToPropertyContainer(view, version);

        return view;
    }

    private ContainerView getPropertyContainer(PropertyDrawEntity propertyDraw, Version version) {
        ContainerSet containers = getPropertyGroupContainer(propertyDraw.getNFToDraw(entity, version)).getContainers();

        ContainerView propertyContainer;
        if (propertyDraw.isNFToolbar(entity, version)) {
            propertyContainer = containers.toolbar;
        } else if (propertyDraw.isNFPopup(entity, version)) {
            propertyContainer = containers.popup;
        } else
            propertyContainer = getPropGroupContainer(containers, propertyDraw.getNFGroup(version), version);
        return propertyContainer;
    }

    @Override
    public void movePropertyDraw(PropertyDrawView property, ComplexLocation<PropertyDrawView> location, Version version) {
        super.movePropertyDraw(property, location, version);

        moveInPropertyContainer(property, location, version);
    }

    @Override
    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroup, Version version) {
        RegularFilterGroupView view = super.addRegularFilterGroup(filterGroup, version);
        addRegularFilterGroup(view, version);
        return view;
    }

    private void addRegularFilterGroup(RegularFilterGroupView filterGroup, Version version) {
        ContainerView filterContainer = getFilterGroupsContainer(filterGroup, version);
        if (filterContainer != null) {
            filterContainer.add(filterGroup, version);
        }
    }

    private ContainerView getFilterGroupsContainer(RegularFilterGroupView filterGroup, Version version) {
        GroupObjectEntity groupObject = filterGroup.entity.getNFToDraw(entity, version);
        return getFilterGroupsContainer(groupObject);
    }

    @Override
    public RegularFilterView addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, Version version) {
        RegularFilterGroupView filterGroupView = get(filterGroup);
        boolean moveGroupToNewContainer = false;
        ContainerView oldContainer = filterGroupView.getNFContainer(version);
        if (oldContainer == null || oldContainer == getFilterGroupsContainer(filterGroupView, version)) {
            //if old container was null or group remains in the default container, move it to the new default container
            moveGroupToNewContainer = true;
        }

        RegularFilterView filterView = super.addRegularFilter(filterGroup, filter, version);
        if (moveGroupToNewContainer) {
            addRegularFilterGroup(filterGroupView, version);
        }
        return filterView;
    }

    private ContainerView getPropGroupContainer(ContainerSet containers, Group currentGroup, Version version) {
        if (currentGroup == null)
            return containers.group;

        // first we'll create containers for upper groups to get right component order
        ContainerView parentGroupContainer = getPropGroupContainer(containers, currentGroup.getNFParent(version, true), version);

        if (!currentGroup.createContainer())
            return parentGroupContainer;

        return containers.groupProps.apply(currentGroup);
    }

    public Function<Group, ContainerView> getGroupProps(ContainerView panel, PropertyContainersView propertyContainer)  {
        final Map<Group, ContainerView> map = new HashMap<>();
        return (Group group) -> {
            synchronized (map) {
                ContainerView<?> propGroupContainer = map.get(group);
                if (propGroupContainer == null) {
                    propGroupContainer = createContainer(group.caption, group.getName(), group.getDebugPoint(), containerFactory, Version.global());
                    setComponentSID(propGroupContainer, getPropGroupContainerSID(group, propertyContainer), Version.global());
                    propGroupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, Version.global());
                    panel.add(propGroupContainer, Version.global());

                    propGroupContainer.setAddParentPC(propertyContainer, pcv -> pcv.getContainers().groupProps.apply(group));
                }

                map.put(group, propGroupContainer);
                return propGroupContainer;
            }
        };
    }

    private static String getPropGroupContainerSID(Group currentGroup, PropertyContainersView propertyContainer) {
        String propertyGroupName = currentGroup.getCanonicalName();
        String currentGroupContainerSID;
        if (propertyContainer instanceof DefaultFormView)
            currentGroupContainerSID = DefaultFormView.getGroupContainerSID(propertyGroupName);
        else
            currentGroupContainerSID = GroupObjectContainerSet.GROUP_CONTAINER + "(" + propertyGroupName + "," + ((PropertyGroupContainersView)propertyContainer).getPropertyGroupContainerSID() + ")";
        return currentGroupContainerSID;
    }

    public void addForm(FormView src, ObjectMapping mapping, Version version) {
        ContainerView mainContainer = mapping.get(src.mainContainer);
        addToObjectsContainer(mainContainer, version);

        // maybe later should be moved to resolving
        String formName = src.entity.getName();
        updateContainerSID(mainContainer, formName);
        if(src instanceof DefaultFormView) {
            for(ContainerView container : ((DefaultFormView) src).getContainers().getContainers())
                if(container != null)
                    updateContainerSID(container, formName);
        }
    }

    private void updateContainerSID(ContainerView container, String formName) {
        container.setSID(container.getSID() + "(" + getFormSID(formName) + ")");
    }

    public DefaultFormView(DefaultFormView src, ObjectMapping mapping) {
        super(src, mapping);

        containers = mapping.get(src.containers);
    }
    // no extends and add

    @Override
    public DefaultFormView copy(ObjectMapping mapping) {
        return new DefaultFormView(this, mapping);
    }
}
