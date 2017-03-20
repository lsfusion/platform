package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

import static lsfusion.interop.form.layout.ContainerConstants.*;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
public class GroupObjectContainerSet {
    private ContainerView groupContainer;
    private ContainerView gridContainer;
    private ContainerView panelContainer;
    private ContainerView panelPropsContainer;
    private ContainerView controlsContainer;
    private ContainerView rightControlsContainer;
    private ContainerView filtersContainer;
    private ContainerView toolbarPropsContainer;

    public ContainerView getGroupContainer() {
        return groupContainer;
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

    public ContainerView getRightControlsContainer() {
        return rightControlsContainer;
    }

    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    public ContainerView getToolbarPropsContainer() {
        return toolbarPropsContainer;
    }

    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory) {
        return create(group, factory, ContainerAdder.<ContainerView, ComponentView, LocalizedString>DEFAULT()); 
    }
    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory, 
                                                 ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();

        set.groupContainer = factory.createContainer(); // контейнер всей группы
        set.groupContainer.setCaption(group.getCaption());
        set.groupContainer.setDescription(LocalizedString.create("{form.layout.group.objects}"));
        set.groupContainer.setSID(group.getSID() + GROUP_CONTAINER);

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription(LocalizedString.create("{form.layout.grid.part}"));
        set.gridContainer.setSID(group.getSID() + GRID_CONTAINER);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(LocalizedString.create("{form.layout.panel}"));
        set.panelContainer.setSID(group.getSID() + PANEL_CONTAINER);

        set.panelPropsContainer = factory.createContainer();
        set.panelPropsContainer.setSID(group.getSID() + PANEL_PROPS_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(LocalizedString.create("{form.layout.control.objects}"));
        set.controlsContainer.setSID(group.getSID() + CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.toolbarPropsContainer.setSID(group.getSID() + TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(LocalizedString.create("{form.layout.filters.container}"));
        set.filtersContainer.setSID(group.getSID() + FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(group.getSID() + CONTROLS_RIGHT_CONTAINER);

        set.groupContainer.setType(ContainerType.CONTAINERV);
        set.groupContainer.setChildrenAlignment(Alignment.LEADING);
        set.groupContainer.setAlignment(FlexAlignment.STRETCH);
        set.groupContainer.setFlex(1);
        adder.add(set.groupContainer, set.gridContainer);
        adder.add(set.groupContainer, set.controlsContainer);
        adder.add(set.groupContainer, group.getFilter());
        adder.add(set.groupContainer, set.panelContainer);

        set.gridContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridContainer.setFlex(1);
        adder.add(set.gridContainer, group.getGrid());

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.controlsContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.controlsContainer, group.getToolbar());
        adder.add(set.controlsContainer, set.rightControlsContainer);

        set.rightControlsContainer.setType(ContainerType.CONTAINERH);
        set.rightControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.rightControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        set.rightControlsContainer.setFlex(1);
        adder.add(set.rightControlsContainer, set.filtersContainer);
        adder.add(set.rightControlsContainer, set.toolbarPropsContainer);
        adder.add(set.rightControlsContainer, group.getShowType());

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

        group.getToolbar().setAlignment(FlexAlignment.CENTER);
        group.getToolbar().setMargin(2);
        group.getShowType().setAlignment(FlexAlignment.CENTER);
        group.getShowType().setMargin(2);

        return set;
    }
}
