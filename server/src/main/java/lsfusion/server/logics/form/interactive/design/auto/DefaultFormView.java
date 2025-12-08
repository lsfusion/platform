package lsfusion.server.logics.form.interactive.design.auto;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormContainerSet;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupContainerSet;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DefaultFormView extends FormView {
    private PropertyGroupContainerView getPropertyContainer(PropertyDrawEntity property, Version version) {
        return getPropertyGroupContainer(property.getNFToDraw(entity, version));
    }

    public static int GROUP_CONTAINER_LINES_COUNT = 3;

    private PropertyGroupContainerView getPropertyGroupContainer(GroupObjectEntity groupObject) {
        if(groupObject == null)
            return null;
        if (groupObject.isInTree())
            return get(groupObject.treeGroup);
        return get(groupObject);
    }

    public static class ContainerSet {
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

        public ContainerSet(ContainerSet containers, ObjectMapping mapping) {
            this(mapping.get(containers.box), mapping.get(containers.panel), mapping.get(containers.group), mapping.get(containers.toolbarBox), mapping.get(containers.toolbar), mapping.get(containers.popup), mapping.get(containers.toolbarLeft), mapping.get(containers.toolbarRight), mapping.get(containers.filterBox), mapping.get(containers.filterGroups), mapping.get(containers.filters),
                    group -> mapping.get(containers.groupProps.apply(group)));
        }
        public ContainerSet(DefaultFormView form, PropertyGroupContainerView groupView, ContainerView box, ContainerView panel, ContainerView group, ContainerView toolbarBox, ContainerView toolbar, ContainerView popup, ContainerView toolbarLeft, ContainerView toolbarRight, ContainerView filterBox, ContainerView filterGroups, ContainerView filters, Version version) {
            this(box, panel, group, toolbarBox, toolbar, popup, toolbarLeft, toolbarRight, filterBox, filterGroups, filters, form.getGroupProps(panel, groupView));

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
    }

    public ContainerSet getContainers(PropertyGroupContainerView view) {
        if (view == null)
            return containers;
        return view.getContainers();
    }
    public ContainerView getBoxContainer(PropertyGroupContainerView groupObject) { return getContainers(groupObject).box; }
    public ContainerView getBoxContainer(GroupObjectEntity groupObject) { return getBoxContainer(get(groupObject)); }

    public ContainerView getPanelPropsContainer(PropertyDrawEntity property, Version version) { return getContainers(getPropertyContainer(property, version)).group; }
    public ContainerView getToolbarBoxContainer(PropertyGroupContainerView groupObject) { return getContainers(groupObject).toolbarBox; }
    public ContainerView getToolbarPropsContainer(PropertyDrawEntity property, Version version) { return getContainers(getPropertyContainer(property, version)).toolbar; }
    public Function<Group, ContainerView> getGroupPropsContainer(PropertyDrawEntity property, Version version) { return getContainers(getPropertyContainer(property, version)).groupProps; }
    public ContainerView getToolbarBoxContainer(GroupObjectEntity groupObject) { return getToolbarBoxContainer(get(groupObject)); }
    public ContainerView getToolbarLeftContainer(PropertyGroupContainerView groupObject) { return getContainers(groupObject).toolbarLeft; }
    public ContainerView getToolbarRightContainer(PropertyGroupContainerView groupObject) { return getContainers(groupObject).toolbarRight; }
    public ContainerView getPopupContainer(PropertyGroupContainerView groupObject) { return getContainers(groupObject).popup; }
    public ContainerView getPopupPropsContainer(PropertyDrawEntity property, Version version) { return getPopupContainer(getPropertyContainer(property, version)); }
    public ContainerView getFilterGroupsContainer(GroupObjectEntity groupObject) { return getContainers(getPropertyGroupContainer(groupObject)).filterGroups; }

    // the main idea that it should be global so all the modules would see it to use the same container for the same property groups
    protected transient final Table<Optional<PropertyGroupContainerView>, Group, ContainerView> groupPropertyContainers = HashBasedTable.create();

    public ContainerSet containers;

    public DefaultFormView(FormEntity formEntity, LocalizedString caption, String imagePath, DefaultFormView src, ObjectMapping mapping, Version version) {
        super(formEntity, caption, imagePath, src, mapping, version);

        if (src != null) {
            containers = new ContainerSet(src.containers, mapping);
        } else {
            FormContainerSet formSet = FormContainerSet.fillContainers(mainContainer, containerFactory, version);
            containers = formSet.getContainerSet(this, null, version);
        }
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

    public void setupFormButtons(Version version) {
        PropertyDrawView editFunction = get(entity.editActionPropertyDraw);
        setupFormButton(editFunction);

        PropertyDrawView dropFunction = get(entity.dropActionPropertyDraw);
        setupFormButton(dropFunction);

        PropertyDrawView refreshFunction = get(entity.refreshActionPropertyDraw);
        setupFormButton(refreshFunction);
        refreshFunction.drawAsync = true;

        PropertyDrawView applyFunction = get(entity.applyActionPropertyDraw);
        setupFormButton(applyFunction);

        PropertyDrawView cancelFunction = get(entity.cancelActionPropertyDraw);
        setupFormButton(cancelFunction);

        PropertyDrawView okFunction = get(entity.okActionPropertyDraw);
        setupFormButton(okFunction);

        PropertyDrawView closeFunction = get(entity.closeActionPropertyDraw);
        setupFormButton(closeFunction);

        PropertyDrawView shareFunction = null;
        if(entity.shareActionPropertyDraw != null) {
            shareFunction = get(entity.shareActionPropertyDraw);
            setupFormButton(shareFunction);
        }

        PropertyDrawView customizeFunction = null;
        if(entity.customizeActionPropertyDraw != null) {
            customizeFunction = get(entity.customizeActionPropertyDraw);
            setupFormButton(customizeFunction);
        }

        PropertyDrawView logMessage = get(entity.logMessagePropertyDraw);

        ContainerView toolbarLeftContainer = getToolbarLeftContainer(null);
        ContainerView toolbarRightContainer = getToolbarRightContainer(null);
        ContainerView popupContainer = getPopupContainer(null);

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
            popupContainer.add(shareFunction, version);
        if(customizeFunction != null)
            popupContainer.add(customizeFunction, version);
    }

    private void setupFormButton(PropertyDrawView action) {
        action.showChangeKey = false;
        action.showChangeMouse = false;
        action.focusable = false;
        action.entity.setEditType(PropertyEditType.EDITABLE);
        action.setAlignment(FlexAlignment.STRETCH);
    }

    private void addToObjectsContainer(ContainerView boxContainer, ComplexLocation<GroupObjectEntity> location, Version version) {
        ComplexLocation<ComponentView> mappedLocation = location.map(neighbour -> getBoxContainer(neighbour.isInTree() ? get(neighbour.treeGroup) : get(neighbour)));

        getBoxContainer((PropertyGroupContainerView) null).addOrMove(boxContainer, mappedLocation, version);
    }

    private void addComponentsToMapping(ContainerSet containers, Version version) {
        for(ContainerView container : containers.getContainers())
            if(container != null)
                addComponentToMapping(container, version);
    }

    // добавление в панель по сути, так как добавление в grid происходит уже на живой форме

    @Override
    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, ComplexLocation<GroupObjectEntity> location, Version version) {
        GroupObjectView view = super.addGroupObject(groupObject, location, version);

        if(!view.entity.isInTree()) {
            GroupObjectContainerSet groupSet = GroupObjectContainerSet.create(view, containerFactory, version);
            view.containers = groupSet.getContainerSet(this, view, version);

            addToObjectsContainer(groupSet.getBoxContainer(), location, version);
            if (view.entity.isPanel()) { // если groupObject идет в панель, то grid'а быть не может, и если box не выставить не 0, он не будет брать весь размер
                groupSet.getBoxContainer().setFlex(0);
            }
        }

        return view;
    }

    @Override
    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, ComplexLocation<GroupObjectEntity> location, Version version) {
        TreeGroupView view = super.addTreeGroup(treeGroup, location, version);

        TreeGroupContainerSet treeSet = TreeGroupContainerSet.create(view, containerFactory, version);
        view.containers = treeSet.getContainerSet(this, view, version);

        addToObjectsContainer(treeSet.getBoxContainer(), location, version);
        return view;
    }

    @Override
    public PropertyDrawView addPropertyDraw(PropertyDrawEntity propertyDraw, ComplexLocation<PropertyDrawView> location, Version version) {
        PropertyDrawView view = super.addPropertyDraw(propertyDraw, location, version);

        PropertyDrawEntity drawEntity = view.entity;
        drawEntity.proceedDefaultDesign(view, this);

        ContainerView propertyContainer;
        if (view.entity.isToolbar(entity)) {
            propertyContainer = getToolbarPropsContainer(drawEntity, version);
        } else if (view.entity.isPopup(entity)) {
            propertyContainer = getPopupPropsContainer(drawEntity, version);
        } else {
            propertyContainer = getPropGroupContainer(drawEntity, view.entity.getNFGroup(version), version);
        }

        propertyContainer.add(view, version);

        return view;
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


    //возвращает контейнер группы и контейнер свойств этой группы
    private ContainerView getPropGroupContainer(PropertyDrawEntity propertyDraw, Group currentGroup, Version version) {
        if (currentGroup == null)
            return getPanelPropsContainer(propertyDraw, version);

        // first we'll create containers for upper groups to get right component order
        ContainerView parentGroupContainer = getPropGroupContainer(propertyDraw, currentGroup.getNFParent(version), version);

        if (!currentGroup.createContainer())
            return parentGroupContainer;

        return getGroupPropsContainer(propertyDraw, version).apply(currentGroup);
    }

    public Function<Group, ContainerView> getGroupProps(ContainerView panel, PropertyGroupContainerView propertyContainer)  {
        final Map<Group, ContainerView> map = new HashMap<>();
        return (Group group) -> {
            synchronized (map) {
                ContainerView propGroupContainer = map.get(group);
                if (propGroupContainer == null) {
                    propGroupContainer = createContainer(group.caption, group.getName(), group.getDebugPoint(), containerFactory);
                    setComponentSID(propGroupContainer, getPropGroupContainerSID(group, propertyContainer), Version.global());
                    propGroupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);
                    panel.add(propGroupContainer, Version.global());
                }

                map.put(group, propGroupContainer);
                return propGroupContainer;
            }
        };
    }

    private static String getPropGroupContainerSID(Group currentGroup, PropertyGroupContainerView propertyContainer) {
        String propertyGroupName = currentGroup.getCanonicalName();
        String currentGroupContainerSID;
        if (propertyContainer == null)
            currentGroupContainerSID = DefaultFormView.getGroupContainerSID(propertyGroupName);
        else
            currentGroupContainerSID = GroupObjectContainerSet.GROUP_CONTAINER + "(" + propertyGroupName + "," + propertyContainer.getPropertyGroupContainerSID() + ")";
        return currentGroupContainerSID;
    }

    @Override
    public void addForm(FormView src, ObjectMapping mapping) {
        super.addForm(src, mapping);

        addToObjectsContainer(src.mainContainer, ComplexLocation.DEFAULT(), mapping.version);

        // todo: we need to change containers sid's
    }

    @Override
    public void copy(FormView src, ObjectMapping mapping) {
        super.copy(src, mapping);

/*        for(Map.Entry<PropertyGroupContainerView, ContainerView> e : src.groupPropertyContainers.entrySet()) {
            PropertyGroupContainerView v = e.getKey() instanceof GroupObjectView ?
                    mapping.get((GroupObjectView) e.getKey()) :
                    mapping.get((TreeGroupView) e.getKey());
            this.groupPropertyContainers.put(v, mapping.get(e.getValue()));
        }*/

    }
}
