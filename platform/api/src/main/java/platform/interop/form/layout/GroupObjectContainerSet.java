package platform.interop.form.layout;

import java.awt.*;

import static platform.base.ApiResourceBundle.getString;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
public class GroupObjectContainerSet<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    public static final String GROUP_CONTAINER = ".box";
    public static final String TREE_GROUP_CONTAINER = ".box";
    public static final String GRID_CONTAINER = ".grid.box";
    public static final String PANEL_CONTAINER = ".panel";
    public static final String FILTER_CONTAINER = ".filters";
    public static final String MANAGER_OBJECT = ".controls";

    private C groupContainer;
    private C gridContainer;
    private C panelContainer;
    private C controlsContainer;
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
        set.groupContainer.add((T) set.gridContainer);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription(getString("form.layout.panel"));
        set.panelContainer.setSID(group.getSID() + PANEL_CONTAINER);
        set.groupContainer.add((T) set.panelContainer);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(getString("form.layout.control.objects"));
        set.controlsContainer.setSID(group.getSID() + MANAGER_OBJECT);
        set.controlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.getConstraints().insetsInside = new Insets(0, 0, 0, 0);
        set.controlsContainer.getConstraints().insetsSibling = new Insets(0, 0, 0, 0);
        set.panelContainer.add((T) set.controlsContainer);

        set.filterContainer = factory.createContainer(); // контейнер фильтров
        set.filterContainer.setDescription(getString("form.layout.filter.container"));
        set.filterContainer.setSID(group.getSID() + FILTER_CONTAINER);
        set.filterContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.add((T) set.filterContainer);

        set.gridContainer.add(group.getGrid());

        group.getShowType().getConstraints().directions = new SimplexComponentDirections(0.01, 0, 0, 0.01);
        set.controlsContainer.add(group.getShowType());

        return set;
    }

}
