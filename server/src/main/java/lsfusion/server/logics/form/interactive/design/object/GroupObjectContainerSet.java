package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
// сейчас полный клон TreeGroupContainerSet, потом надо рефакторить
public class GroupObjectContainerSet {
    public static final String BOX_CONTAINER = "BOX";
        public static final String FILTERS_CONTAINER = "FILTERS";
        public static final String GRID_COMPONENT = "GRID";
        public static final String TOOLBARBOX_CONTAINER = "TOOLBARBOX";
            public static final String TOOLBARLEFT_CONTAINER = "TOOLBARLEFT";
                public static final String TOOLBAR_SYSTEM_COMPONENT = "TOOLBARSYSTEM";
            public static final String TOOLBARRIGHT_CONTAINER = "TOOLBARRIGHT";
                public static final String FILTERGROUPS_CONTAINER = "FILTERGROUPS";
                    public static final String FILTERGROUP_COMPONENT = "FILTERGROUP";
                public static final String TOOLBAR_CONTAINER = "TOOLBAR";
        public static final String PANEL_CONTAINER = "PANEL";
            public static final String GROUP_CONTAINER = "GROUP";
    
    private ContainerView boxContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView filterGroupsContainer;
    private ContainerView toolbarContainer;

    public ContainerView getBoxContainer() {
        return boxContainer;
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

    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory, Version version) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();
        String sid = group.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer(); // контейнер всей группы
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));
        set.boxContainer.setCaption(group.getCaption());

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.filterGroupsContainer = factory.createContainer(); // контейнер фильтров
        set.filterGroupsContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = factory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = factory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setType(ContainerType.CONTAINERV);
        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);
        set.boxContainer.add(group.filtersContainer, version);
        set.boxContainer.add(group.getGrid(), version);
        set.boxContainer.add(set.toolbarBoxContainer, version);
        set.boxContainer.add(set.panelContainer, version);

        set.toolbarBoxContainer.setType(ContainerType.CONTAINERH);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.add(set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(set.toolbarRightContainer, version);

        set.toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarLeftContainer.add(group.getToolbarSystem(), version);

        set.toolbarRightContainer.setType(ContainerType.CONTAINERH);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarRightContainer.setChildrenAlignment(FlexAlignment.END);
        set.toolbarRightContainer.setFlex(1);
        set.toolbarRightContainer.add(group.getCalculations(), version);
        set.toolbarRightContainer.add(set.filterGroupsContainer, version);
        set.toolbarRightContainer.add(set.toolbarContainer, version);

        set.filterGroupsContainer.setType(ContainerType.CONTAINERH);
        set.filterGroupsContainer.setAlignment(FlexAlignment.CENTER);
        set.filterGroupsContainer.setChildrenAlignment(FlexAlignment.END);

        set.toolbarContainer.setType(ContainerType.CONTAINERH);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH);

        set.panelContainer.setType(ContainerType.CONTAINERV);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.add(set.groupContainer, version);

        set.groupContainer.setType(ContainerType.COLUMNS);
        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);

        group.getToolbarSystem().setMargin(2);
        group.getToolbarSystem().setAlignment(FlexAlignment.CENTER);
        group.getCalculations().setFlex(1);
        group.getCalculations().setAlignment(FlexAlignment.CENTER);

        return set;
    }
}
