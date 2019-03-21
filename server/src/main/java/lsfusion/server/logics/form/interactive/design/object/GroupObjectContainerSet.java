package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.Alignment;
import lsfusion.interop.form.design.ContainerAdder;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
// сейчас полный клон TreeGroupContainerSet, потом надо рефакторить
public class GroupObjectContainerSet {
    public static final String BOX_CONTAINER = "BOX";
        public static final String GRIDBOX_CONTAINER = "GRIDBOX";
            public static final String CLASSCHOOSER_COMPONENT = "CLASSCHOOSER";
            public static final String GRID_COMPONENT = "GRID";
        public static final String TOOLBARBOX_CONTAINER = "TOOLBARBOX";
            public static final String TOOLBARLEFT_CONTAINER = "TOOLBARLEFT";
                public static final String TOOLBAR_SYSTEM_COMPONENT = "TOOLBARSYSTEM";
            public static final String TOOLBARRIGHT_CONTAINER = "TOOLBARRIGHT";
                public static final String FILTERGROUPS_CONTAINER = "FILTERGROUPS";
                    public static final String FILTERGROUP_COMPONENT = "FILTERGROUP";
                public static final String TOOLBAR_CONTAINER = "TOOLBAR";
                public static final String SHOWTYPE_COMPONENT = "SHOWTYPE";
        public static final String USERFILTER_COMPONENT = "USERFILTER";
        public static final String PANEL_CONTAINER = "PANEL";
            public static final String GROUP_CONTAINER = "GROUP";
    
    private ContainerView boxContainer;
    private ContainerView gridBoxContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView filtersContainer;
    private ContainerView toolbarContainer;

    public ContainerView getBoxContainer() {
        return boxContainer;
    }

    public ContainerView getGridBoxContainer() {
        return gridBoxContainer;
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

    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory,
                                                 ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();
        String sid = group.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer(); // контейнер всей группы
        set.boxContainer.setCaption(group.getCaption());
        set.boxContainer.setDescription(LocalizedString.create("{form.layout.group.objects}"));
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));

        set.gridBoxContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridBoxContainer.setDescription(LocalizedString.create("{form.layout.grid.part}"));
        set.gridBoxContainer.setSID(DefaultFormView.getGridBoxContainerSID(sid));

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(LocalizedString.create("{form.layout.panel}"));
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setDescription(LocalizedString.create("{form.layout.control.objects}"));
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(LocalizedString.create("{form.layout.filters.container}"));
        set.filtersContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = factory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = factory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setType(ContainerType.CONTAINERV);
        set.boxContainer.setChildrenAlignment(Alignment.START);
        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);
        adder.add(set.boxContainer, set.gridBoxContainer);
        adder.add(set.boxContainer, set.toolbarBoxContainer);
        adder.add(set.boxContainer, group.getUserFilter());
        adder.add(set.boxContainer, set.panelContainer);

        set.gridBoxContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridBoxContainer.setFlex(1);
        adder.add(set.gridBoxContainer, group.getGrid());

        set.toolbarBoxContainer.setType(ContainerType.CONTAINERH);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.setChildrenAlignment(Alignment.START);
        adder.add(set.toolbarBoxContainer, set.toolbarLeftContainer);
        adder.add(set.toolbarBoxContainer, set.toolbarRightContainer);

        set.toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarLeftContainer.setChildrenAlignment(Alignment.END);
        adder.add(set.toolbarLeftContainer, group.getToolbarSystem());

        set.toolbarRightContainer.setType(ContainerType.CONTAINERH);
        set.toolbarRightContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarRightContainer.setChildrenAlignment(Alignment.END);
        set.toolbarRightContainer.setFlex(1);
        adder.add(set.toolbarRightContainer, group.getCalculations());
        adder.add(set.toolbarRightContainer, set.filtersContainer);
        adder.add(set.toolbarRightContainer, set.toolbarContainer);
        adder.add(set.toolbarRightContainer, group.getShowType());

        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.CENTER);
        set.filtersContainer.setChildrenAlignment(Alignment.END);

        set.toolbarContainer.setType(ContainerType.CONTAINERH);
        set.toolbarContainer.setAlignment(FlexAlignment.CENTER);

        set.panelContainer.setType(ContainerType.CONTAINERV);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.setChildrenAlignment(Alignment.START);
        adder.add(set.panelContainer, set.groupContainer);

        set.groupContainer.setType(ContainerType.COLUMNS);
        set.groupContainer.setColumns(4);

        group.getToolbarSystem().setMargin(2);
        group.getToolbarSystem().setAlignment(FlexAlignment.CENTER);
        group.getUserFilter().setAlignment(FlexAlignment.STRETCH);
        group.getCalculations().setFlex(1);
        group.getCalculations().setAlignment(FlexAlignment.CENTER);
        group.getShowType().setAlignment(FlexAlignment.CENTER);
        group.getShowType().setMargin(2);

        return set;
    }
}
