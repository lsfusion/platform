package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

import static lsfusion.interop.form.layout.ContainerConstants.*;

// сейчас полный клон GroupObjectContainerSet, потом надо рефакторить
public class TreeGroupContainerSet {

    private ContainerView boxContainer;
    private ContainerView gridContainer;
    private ContainerView panelContainer;
    private ContainerView panelPropsContainer;
    private ContainerView controlsContainer;
    private ContainerView leftControlsContainer;
    private ContainerView rightControlsContainer;
    private ContainerView filtersContainer;
    private ContainerView toolbarPropsContainer;

    public ContainerView getBoxContainer() {
        return boxContainer;
    }

    public ContainerView getGridContainer() {
        return gridContainer;
    }

    public ContainerView getPanelContainer() {
        return panelContainer;
    }

    public ContainerView getPanelPropsContainer() {
        return panelPropsContainer;
    }

    public ContainerView getControlsContainer() {
        return controlsContainer;
    }

    public ContainerView getLeftControlsContainer() {
        return leftControlsContainer;
    }

    public ContainerView getRightControlsContainer() {
        return rightControlsContainer;
    }

    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    public ContainerView getToolbarPropsContainer() {
        return toolbarPropsContainer;
    }

    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory,
                                               ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {
        TreeGroupContainerSet set = new TreeGroupContainerSet();
        String sid = treeGroup.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer();
        set.boxContainer.setCaption(LocalizedString.create("{form.layout.tree}"));
        set.boxContainer.setDescription(LocalizedString.create("{form.layout.tree}"));
        set.boxContainer.setSID(sid + ContainerConstants.BOX_CONTAINER);

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription(LocalizedString.create("{form.layout.grid.part}"));
        set.gridContainer.setSID(sid + GRID_CONTAINER);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(LocalizedString.create("{form.layout.panel}"));
        set.panelContainer.setSID(sid + PANEL_CONTAINER);

        set.panelPropsContainer = factory.createContainer();
        set.panelPropsContainer.setSID(sid + PANEL_PROPS_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(LocalizedString.create("{form.layout.conrol.objects}"));
        set.controlsContainer.setSID(sid + ContainerConstants.CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.toolbarPropsContainer.setSID(sid + ContainerConstants.TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(LocalizedString.create("{form.layout.filters.container}"));
        set.filtersContainer.setSID(sid + ContainerConstants.FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(sid + ContainerConstants.CONTROLS_RIGHT_CONTAINER);

        set.leftControlsContainer = factory.createContainer();
        set.leftControlsContainer.setSID(sid + ContainerConstants.CONTROLS_LEFT_CONTAINER);

        set.boxContainer.setType(ContainerType.CONTAINERV);
        set.boxContainer.setChildrenAlignment(Alignment.LEADING);
        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);
        adder.add(set.boxContainer, set.gridContainer);
        adder.add(set.boxContainer, set.controlsContainer);
        adder.add(set.boxContainer, treeGroup.getFilter());
        adder.add(set.boxContainer, set.panelContainer);

        set.gridContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridContainer.setFlex(1);
        adder.add(set.gridContainer, treeGroup);

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.controlsContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.controlsContainer, set.leftControlsContainer);
        adder.add(set.controlsContainer, set.rightControlsContainer);

        set.leftControlsContainer.setType(ContainerType.CONTAINERH);
        set.leftControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.leftControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        adder.add(set.leftControlsContainer, treeGroup.getToolbar());

        set.rightControlsContainer.setType(ContainerType.CONTAINERH);
        set.rightControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.rightControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        set.rightControlsContainer.setFlex(1);
        adder.add(set.rightControlsContainer, set.filtersContainer);
        adder.add(set.rightControlsContainer, set.toolbarPropsContainer);


        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.CENTER);
        set.filtersContainer.setChildrenAlignment(Alignment.TRAILING);

        set.toolbarPropsContainer.setType(ContainerType.CONTAINERH);
        set.toolbarPropsContainer.setAlignment(FlexAlignment.CENTER);

        set.panelContainer.setType(ContainerType.CONTAINERV);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.panelContainer, set.panelPropsContainer);

        set.panelPropsContainer.setType(ContainerType.COLUMNS);
        set.panelPropsContainer.setColumns(4);

        treeGroup.getToolbar().setMargin(2);
        treeGroup.getToolbar().setAlignment(FlexAlignment.CENTER);
        treeGroup.getFilter().setAlignment(FlexAlignment.STRETCH);


        return set;
    }
}
