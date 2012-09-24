package platform.interop.form.layout;

import java.awt.*;

import static platform.base.ApiResourceBundle.getString;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
public class GroupObjectContainerSet<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    public static final String GROUP_CONTAINER = ".box";
    public static final String TREE_GROUP_CONTAINER = ".box";
    public static final String GRID_CONTAINER = ".grid.box";
    public static final String PANEL_CONTAINER = ".panel";
    public static final String FILTERS_CONTAINER = ".filters";
    public static final String TOOLBAR_PROPS_CONTAINER = ".toolbar.props.box";
    public static final String CONTROLS_CONTAINER = ".controls";
    public static final String CONTROLS_RIGHT_CONTAINER = ".controls.right";
    public static final String TOOLBAR_CONTAINER = ".toolbar.box";
    public static final String SHOWTYPE_CONTAINER = ".showtype.box";
    public static final String FILTER_CONTAINER = ".filter.box";

    private C groupContainer;
    private C gridContainer;
    private C panelContainer;
    private C controlsContainer;
    private C rightControlsContainer;
    private C filtersContainer;
    private C toolbarPropsContainer;

    private C toolbarContainer;
    private C showTypeContainer;
    private C filterContainer;

    public C getGroupContainer() {
        return groupContainer;
    }

    public C getGridContainer() {
        return gridContainer;
    }

    public C getPanelContainer() {
        return panelContainer;
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

    public C getShowTypeContainer() {
        return showTypeContainer;
    }

    public C getToolbarContainer() {
        return toolbarContainer;
    }

    public C getFilterContainer() {
        return filterContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> GroupObjectContainerSet<C, T> create(AbstractGroupObject<T> group, ContainerFactory<C> factory) {

        GroupObjectContainerSet<C, T> set = new GroupObjectContainerSet<C, T>();

        set.groupContainer = factory.createContainer(); // контейнер всей группы
        // та же логика с Title еще есть в ContainerRemover 
        set.groupContainer.setTitle(group.getCaption());
        set.groupContainer.setDescription(getString("form.layout.group.objects"));
        set.groupContainer.setSID(group.getSID() + GROUP_CONTAINER);
        set.groupContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription(getString("form.layout.grid.part"));
        set.gridContainer.setSID(group.getSID() + GRID_CONTAINER);
        set.gridContainer.setType(ContainerType.SPLIT_PANE_HORIZONTAL);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(getString("form.layout.panel"));
        set.panelContainer.setSID(group.getSID() + PANEL_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(getString("form.layout.control.objects"));
        set.controlsContainer.setSID(group.getSID() + CONTROLS_CONTAINER);
        set.controlsContainer.getConstraints().insetsInside = new Insets(2, 2, 2, 2);
        set.controlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(getString("form.layout.toolbar.props.container"));
        set.toolbarPropsContainer.setSID(group.getSID() + TOOLBAR_PROPS_CONTAINER);
        set.toolbarPropsContainer.getConstraints().insetsInside = new Insets(0, 2, 2, 2);
        set.toolbarPropsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.toolbarPropsContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(getString("form.layout.filters.container"));
        set.filtersContainer.setSID(group.getSID() + FILTERS_CONTAINER);
        set.filtersContainer.getConstraints().insetsInside = new Insets(0, 2, 1, 2);
        set.filtersContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.filtersContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(group.getSID() + CONTROLS_RIGHT_CONTAINER);
        set.rightControlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.rightControlsContainer.getConstraints().insetsInside = new Insets(0, 0, 0, 0);
        set.rightControlsContainer.getConstraints().directions = new SimplexComponentDirections(0.0, -0.01, 0.0, 0.01);

        set.filterContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.filterContainer.setDescription(getString("form.layout.filter.container"));
        set.filterContainer.setSID(group.getSID() + FILTER_CONTAINER);
        set.filterContainer.getConstraints().insetsInside = new Insets(0, 1, 2, 1);
        set.filterContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        set.showTypeContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.showTypeContainer.setDescription(getString("form.layout.filter.container"));
        set.showTypeContainer.setSID(group.getSID() + SHOWTYPE_CONTAINER);
        set.showTypeContainer.getConstraints().insetsInside = new Insets(0, 2, 2, 2);
        set.showTypeContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.showTypeContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);

        set.toolbarContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarContainer.setDescription(getString("form.layout.filter.container"));
        set.toolbarContainer.setSID(group.getSID() + TOOLBAR_CONTAINER);
        set.toolbarContainer.getConstraints().insetsInside = new Insets(1, 0, 2, 2);
        set.toolbarContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.toolbarContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.01, 0.0, 0.0);

        set.showTypeContainer.add(group.getShowType());

        set.toolbarContainer.add(group.getToolbar());

        set.gridContainer.add(group.getGrid());

        set.rightControlsContainer.add((T) set.filtersContainer);
        set.rightControlsContainer.add((T) set.toolbarPropsContainer);
        set.rightControlsContainer.add((T) set.showTypeContainer);

        set.controlsContainer.add((T) set.toolbarContainer);
        set.controlsContainer.add((T) set.rightControlsContainer);

        set.filterContainer.add(group.getFilter());

        set.groupContainer.add((T) set.gridContainer);
        set.groupContainer.add((T) set.controlsContainer);
        set.groupContainer.add((T) set.filterContainer);
        set.groupContainer.add((T) set.panelContainer);

        return set;
    }
}
