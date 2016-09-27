package lsfusion.interop.form.layout;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
public class GroupObjectContainerSet<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    public static final String GROUP_CONTAINER = ".box";
    public static final String TREE_GROUP_CONTAINER = ".box";
    public static final String GRID_CONTAINER = ".grid.box";
    public static final String PANEL_CONTAINER = ".panel";
    public static final String PANEL_PROPS_CONTAINER = ".panel.props";
    public static final String FILTERS_CONTAINER = ".filters";
    public static final String TOOLBAR_PROPS_CONTAINER = ".toolbar.props.box";
    public static final String CONTROLS_CONTAINER = ".controls";
    public static final String CONTROLS_RIGHT_CONTAINER = ".controls.right";

    private C groupContainer;
    private C gridContainer;
    private C panelContainer;
    private C panelPropsContainer;
    private C controlsContainer;
    private C rightControlsContainer;
    private C filtersContainer;
    private C toolbarPropsContainer;

    public C getGroupContainer() {
        return groupContainer;
    }

    public C getGridContainer() {
        return gridContainer;
    }

    public C getPanelContainer() {
        return panelContainer;
    }

    public C getPanelPropsContainer() {
        return panelPropsContainer;
    }

    public C getControlsContainer() {
        return controlsContainer;
    }

    public C getRightControlsContainer() {
        return rightControlsContainer;
    }

    public C getFiltersContainer() {
        return filtersContainer;
    }

    public C getToolbarPropsContainer() {
        return toolbarPropsContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> GroupObjectContainerSet<C, T> create(AbstractGroupObject<T> group, ContainerFactory<C> factory) {
        return create(group, factory, ContainerAdder.<C, T>DEFAULT()); 
    }
    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> GroupObjectContainerSet<C, T> create(AbstractGroupObject<T> group, ContainerFactory<C> factory, ContainerAdder<C, T> adder) {

        GroupObjectContainerSet<C, T> set = new GroupObjectContainerSet<>();

        set.groupContainer = factory.createContainer(); // контейнер всей группы
        set.groupContainer.setCaption(group.getCaption());
        set.groupContainer.setDescription("{form.layout.group.objects}");
        set.groupContainer.setSID(group.getSID() + GROUP_CONTAINER);

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription("{form.layout.grid.part}");
        set.gridContainer.setSID(group.getSID() + GRID_CONTAINER);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription("{form.layout.panel}");
        set.panelContainer.setSID(group.getSID() + PANEL_CONTAINER);

        set.panelPropsContainer = factory.createContainer();
        set.panelPropsContainer.setSID(group.getSID() + PANEL_PROPS_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription("{form.layout.control.objects}");
        set.controlsContainer.setSID(group.getSID() + CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription("{form.layout.toolbar.props.container}");
        set.toolbarPropsContainer.setSID(group.getSID() + TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription("{form.layout.filters.container}");
        set.filtersContainer.setSID(group.getSID() + FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(group.getSID() + CONTROLS_RIGHT_CONTAINER);

        set.groupContainer.setType(ContainerType.CONTAINERV);
        set.groupContainer.setChildrenAlignment(Alignment.LEADING);
        set.groupContainer.setAlignment(FlexAlignment.STRETCH);
        set.groupContainer.setFlex(1);
        adder.add(set.groupContainer, (T) set.gridContainer);
        adder.add(set.groupContainer, (T) set.controlsContainer);
        adder.add(set.groupContainer, (T) group.getFilter());
        adder.add(set.groupContainer, (T) set.panelContainer);

        set.gridContainer.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
        set.gridContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridContainer.setFlex(1);
        adder.add(set.gridContainer, group.getGrid());

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.controlsContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.controlsContainer, (T) group.getToolbar());
        adder.add(set.controlsContainer, (T) set.rightControlsContainer);

        set.rightControlsContainer.setType(ContainerType.CONTAINERH);
        set.rightControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.rightControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        set.rightControlsContainer.setFlex(1);
        adder.add(set.rightControlsContainer, (T) set.filtersContainer);
        adder.add(set.rightControlsContainer, (T) set.toolbarPropsContainer);
        adder.add(set.rightControlsContainer, (T) group.getShowType());

        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.CENTER);
        set.filtersContainer.setChildrenAlignment(Alignment.TRAILING);

        set.toolbarPropsContainer.setType(ContainerType.CONTAINERH);
        set.toolbarPropsContainer.setAlignment(FlexAlignment.CENTER);

        set.panelContainer.setType(ContainerType.CONTAINERV);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.panelContainer, (T) set.panelPropsContainer);

        set.panelPropsContainer.setType(ContainerType.COLUMNS);
        set.panelPropsContainer.setColumns(4);

        group.getToolbar().setAlignment(FlexAlignment.CENTER);
        group.getToolbar().setMargin(2);
        group.getShowType().setAlignment(FlexAlignment.CENTER);
        group.getShowType().setMargin(2);

        return set;
    }
}
