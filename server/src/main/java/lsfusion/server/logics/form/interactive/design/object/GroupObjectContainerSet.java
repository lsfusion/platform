package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.ContainerFactory;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.debug.DebugInfo;

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
                public static final String PROPERTY_COMPONENT = "PROPERTY";


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

    public DefaultFormView.ContainerSet getContainerSet(DefaultFormView formView, GridView gridView, Version version) {
        return new DefaultFormView.ContainerSet(formView, gridView, boxContainer, panelContainer, groupContainer, toolbarBoxContainer, toolbarContainer, popupContainer, toolbarLeftContainer, toolbarRightContainer, filterBoxContainer, filterGroupsContainer, gridView.filtersContainer, version);
    }

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

    private static ContainerView createContainer(ContainerFactory<ContainerView> factory) {
        return createContainer(factory, null);
    }

    private static ContainerView createContainer(ContainerFactory<ContainerView> factory, DebugInfo.DebugPoint debugPoint) {
        return factory.createContainer(debugPoint);
    }

    public static GroupObjectContainerSet create(GridView grid, ContainerFactory<ContainerView> factory, Version version) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();
        String sid = grid.getPropertyGroupContainerSID();

        set.boxContainer = createContainer(factory, grid.groupObject.entity.getDebugPoint()); // контейнер всей группы
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));
        set.boxContainer.setName(grid.getPropertyGroupContainerName(), version);

        set.filterBoxContainer = createContainer(factory);
        set.filterBoxContainer.setSID(DefaultFormView.getFilterBoxContainerSID(sid));

        set.panelContainer = createContainer(factory); // контейнер панели
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = createContainer(factory);
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = createContainer(factory); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = createContainer(factory); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.popupContainer = createContainer(factory);
        set.popupContainer.setSID(DefaultFormView.getPopupContainerSID(sid));
        set.popupContainer.setPopup(true, version);
        set.popupContainer.setCollapsed(true, version);
        set.popupContainer.setImage("bi bi-three-dots-vertical", null, version);

        set.filterGroupsContainer = createContainer(factory); // контейнер фильтров
        set.filterGroupsContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = createContainer(factory);
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = createContainer(factory);
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.boxContainer.groupObjectBox = grid.groupObject.entity;

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();
        if (toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.boxContainer.add(set.filterBoxContainer, version);
        set.boxContainer.add(grid, version);
        if (!toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.boxContainer.add(set.panelContainer, version);
        
        set.filterBoxContainer.setHorizontal(true, version);
        set.filterBoxContainer.add(grid.filtersContainer, version);
        grid.filtersContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.filterBoxContainer.add(grid.filterControls, version);
        grid.filterControls.setAlignment(FlexAlignment.CENTER, version);

        // we're stretching the intermediate containers, and centering the leaf components
        set.toolbarBoxContainer.setHorizontal(true, version);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarRightContainer : set.toolbarLeftContainer, version);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarLeftContainer : set.toolbarRightContainer, version);
        set.toolbarRightContainer.setFlex(1d, version);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarLeftContainer.setHorizontal(true, version);
        set.toolbarLeftContainer.add(grid.toolbarSystem, version);
        grid.toolbarSystem.setAlignment(FlexAlignment.CENTER, version);

        set.toolbarRightContainer.setHorizontal(true, version);
        set.toolbarRightContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.START : FlexAlignment.END, version);
        set.toolbarRightContainer.add(grid.calculations, version);
        set.toolbarRightContainer.add(set.filterGroupsContainer, version);
        set.filterGroupsContainer.setAlignment(FlexAlignment.STRETCH,version);
        set.toolbarRightContainer.add(set.toolbarContainer, version);
        set.toolbarLeftContainer.add(set.popupContainer, version);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.filterGroupsContainer.setHorizontal(true, version);
        set.filterGroupsContainer.setChildrenAlignment(FlexAlignment.END, version);

        set.toolbarContainer.setHorizontal(true, version);

        set.panelContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.panelContainer.add(set.groupContainer, version);

        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);

        grid.toolbarSystem.setMargin(2, version);
        grid.calculations.setFlex(1d, version);

        return set;
    }
}
