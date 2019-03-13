package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// сейчас полный клон GroupObjectContainerSet, потом надо рефакторить
public class TreeGroupContainerSet {

    private ContainerView boxContainer;
    private ContainerView gridContainer;
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

    public ContainerView getGridContainer() {
        return gridContainer;
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

    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory,
                                               ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {
        TreeGroupContainerSet set = new TreeGroupContainerSet();
        String sid = treeGroup.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer();
        set.boxContainer.setCaption(LocalizedString.create("{form.layout.tree}"));
        set.boxContainer.setDescription(LocalizedString.create("{form.layout.tree}"));
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription(LocalizedString.create("{form.layout.grid.part}"));
        set.gridContainer.setSID(DefaultFormView.getGridBoxContainerSID(sid));

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(LocalizedString.create("{form.layout.panel}"));
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setDescription(LocalizedString.create("{form.layout.conrol.objects}"));
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
        adder.add(set.boxContainer, set.gridContainer);
        adder.add(set.boxContainer, set.toolbarBoxContainer);
        adder.add(set.boxContainer, treeGroup.getUserFilter());
        adder.add(set.boxContainer, set.panelContainer);

        set.gridContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridContainer.setFlex(1);
        adder.add(set.gridContainer, treeGroup);

        set.toolbarBoxContainer.setType(ContainerType.CONTAINERH);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.setChildrenAlignment(Alignment.START);
        adder.add(set.toolbarBoxContainer, set.toolbarLeftContainer);
        adder.add(set.toolbarBoxContainer, set.toolbarRightContainer);

        set.toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarLeftContainer.setChildrenAlignment(Alignment.END);
        adder.add(set.toolbarLeftContainer, treeGroup.getToolbarSystem());

        set.toolbarRightContainer.setType(ContainerType.CONTAINERH);
        set.toolbarRightContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarRightContainer.setChildrenAlignment(Alignment.END);
        set.toolbarRightContainer.setFlex(1);
        adder.add(set.toolbarRightContainer, set.filtersContainer);
        adder.add(set.toolbarRightContainer, set.toolbarContainer);


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

        treeGroup.getToolbarSystem().setMargin(2);
        treeGroup.getToolbarSystem().setAlignment(FlexAlignment.CENTER);
        treeGroup.getUserFilter().setAlignment(FlexAlignment.STRETCH);


        return set;
    }
}
