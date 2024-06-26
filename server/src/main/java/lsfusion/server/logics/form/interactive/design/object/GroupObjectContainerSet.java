package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.admin.Settings;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
// сейчас полный клон TreeGroupContainerSet, потом надо рефакторить
public class GroupObjectContainerSet {
    public static final String BOX_CONTAINER = "BOX";
        public static final String FILTERBOX_CONTAINER = "FILTERBOX";
            public static final String FILTERS_CONTAINER = "FILTERS";
            public static final String FILTER_CONTROLS_COMPONENT = "FILTERCONTROLS";
        public static final String GRID_COMPONENT = "GRID";
        public static final String TOOLBARBOX_CONTAINER = "TOOLBARBOX";
            public static final String TOOLBARLEFT_CONTAINER = "TOOLBARLEFT";
                public static final String TOOLBAR_SYSTEM_COMPONENT = "TOOLBARSYSTEM";
            public static final String TOOLBARRIGHT_CONTAINER = "TOOLBARRIGHT";
                public static final String FILTERGROUPS_CONTAINER = "FILTERGROUPS";
                    public static final String FILTERGROUP_COMPONENT = "FILTERGROUP";
                public static final String TOOLBAR_CONTAINER = "TOOLBAR";
                public static final String POPUP_CONTAINER = "POPUP";
        public static final String PANEL_CONTAINER = "PANEL";
            public static final String GROUP_CONTAINER = "GROUP";
    
    private ContainerView boxContainer;
    private ContainerView filterBoxContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView filterGroupsContainer;
    private ContainerView toolbarContainer;
    private ContainerView popupContainer;

    public ContainerView getBoxContainer() {
        return boxContainer;
    }
    
    public ContainerView getFilterBoxContainer() {
        return filterBoxContainer;
    }

    public ContainerView getPanelContainer() {
        return panelContainer;
    }

    public ContainerView getGroupContainer() {
        return groupContainer;
    }

    public ContainerView getToolbarBoxContainer() {
        return toolbarBoxContainer;
    }

    public ContainerView getToolbarLeftContainer() {
        return toolbarLeftContainer;
    }

    public ContainerView getToolbarRightContainer() {
        return toolbarRightContainer;
    }

    public ContainerView getFilterGroupsContainer() {
        return filterGroupsContainer;
    }

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public ContainerView getPopupContainer() {
        return popupContainer;
    }

    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory, Version version) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();
        String sid = group.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer(); // контейнер всей группы
        set.boxContainer.setDebugPoint(group.entity.getDebugPoint()); //set debugPoint to containers that have a caption
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));
        set.boxContainer.setCaption(group.getCaption());
        set.boxContainer.setName(group.getPropertyGroupContainerName());

        set.filterBoxContainer = factory.createContainer();
        set.filterBoxContainer.setSID(DefaultFormView.getFilterBoxContainerSID(sid));

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.popupContainer = factory.createContainer();
        set.popupContainer.setSID(DefaultFormView.getPopupContainerSID(sid));
        set.popupContainer.setPopup(true);
        set.popupContainer.setImage("bi bi-three-dots-vertical", null);

        set.filterGroupsContainer = factory.createContainer(); // контейнер фильтров
        set.filterGroupsContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = factory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = factory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();
        if (toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.boxContainer.add(set.filterBoxContainer, version);
        set.boxContainer.add(group.getGrid(), version);
        if (!toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.boxContainer.add(set.panelContainer, version);
        
        set.filterBoxContainer.setHorizontal(true);
        set.filterBoxContainer.add(group.filtersContainer, version);
        group.filtersContainer.setAlignment(FlexAlignment.STRETCH);
        set.filterBoxContainer.add(group.filterControls, version);
        group.filterControls.setAlignment(FlexAlignment.CENTER);

        // we're stretching the intermediate containers, and centering the leaf components
        set.toolbarBoxContainer.setHorizontal(true);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarRightContainer : set.toolbarLeftContainer, version);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarLeftContainer : set.toolbarRightContainer, version);
        set.toolbarRightContainer.setFlex(1);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarLeftContainer.setHorizontal(true);
        set.toolbarLeftContainer.add(group.toolbarSystem, version);
        group.toolbarSystem.setAlignment(FlexAlignment.CENTER);

        set.toolbarRightContainer.setHorizontal(true);
        set.toolbarRightContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.START : FlexAlignment.END);
        set.toolbarRightContainer.add(group.getCalculations(), version);
        set.toolbarRightContainer.add(set.filterGroupsContainer, version);
        set.filterGroupsContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarRightContainer.add(set.toolbarContainer, version);
        set.toolbarLeftContainer.add(set.popupContainer, version);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH);

        set.filterGroupsContainer.setHorizontal(true);
        set.filterGroupsContainer.setChildrenAlignment(FlexAlignment.END);

        set.toolbarContainer.setHorizontal(true);

        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.add(set.groupContainer, version);

        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);

        group.getToolbarSystem().setMargin(2);
        group.getCalculations().setFlex(1);

        return set;
    }
}
