package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

import static lsfusion.interop.form.layout.ContainerConstants.*;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
// сейчас полный клон TreeGroupContainerSet, потом надо рефакторить
public class GroupObjectContainerSet {
    private ContainerView boxContainer;
    private ContainerView gridBoxContainer;
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

    public ContainerView getGridBoxContainer() {
        return gridBoxContainer;
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

    public static GroupObjectContainerSet create(GroupObjectView group, ContainerFactory<ContainerView> factory,
                                                 ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {

        GroupObjectContainerSet set = new GroupObjectContainerSet();
        String sid = group.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer(); // контейнер всей группы
        set.boxContainer.setCaption(group.getCaption());
        set.boxContainer.setDescription(LocalizedString.create("{form.layout.group.objects}"));
        set.boxContainer.setSID(sid + BOX_CONTAINER);

        set.gridBoxContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridBoxContainer.setDescription(LocalizedString.create("{form.layout.grid.part}"));
        set.gridBoxContainer.setSID(sid + GRID_CONTAINER);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(LocalizedString.create("{form.layout.panel}"));
        set.panelContainer.setSID(sid + PANEL_CONTAINER);

        set.panelPropsContainer = factory.createContainer();
        set.panelPropsContainer.setSID(sid + PANEL_PROPS_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(LocalizedString.create("{form.layout.control.objects}"));
        set.controlsContainer.setSID(sid + CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.toolbarPropsContainer.setSID(sid + TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(LocalizedString.create("{form.layout.filters.container}"));
        set.filtersContainer.setSID(sid + FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(sid + CONTROLS_RIGHT_CONTAINER);

        set.leftControlsContainer = factory.createContainer();
        set.leftControlsContainer.setSID(sid + CONTROLS_LEFT_CONTAINER);

        set.boxContainer.setType(ContainerType.CONTAINERV);
        set.boxContainer.setChildrenAlignment(Alignment.LEADING);
        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);
        adder.add(set.boxContainer, set.gridBoxContainer);
        adder.add(set.boxContainer, set.controlsContainer);
        adder.add(set.boxContainer, group.getFilter());
        adder.add(set.boxContainer, set.panelContainer);

        set.gridBoxContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridBoxContainer.setFlex(1);
        adder.add(set.gridBoxContainer, group.getGrid());

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.controlsContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.controlsContainer, set.leftControlsContainer);
        adder.add(set.controlsContainer, set.rightControlsContainer);

        set.leftControlsContainer.setType(ContainerType.CONTAINERH);
        set.leftControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.leftControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        adder.add(set.leftControlsContainer, group.getToolbar());

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

        group.getToolbar().setMargin(2);
        group.getToolbar().setAlignment(FlexAlignment.CENTER);
        group.getFilter().setAlignment(FlexAlignment.STRETCH);
        group.getShowType().setAlignment(FlexAlignment.CENTER);
        group.getShowType().setMargin(2);

        return set;
    }
}
