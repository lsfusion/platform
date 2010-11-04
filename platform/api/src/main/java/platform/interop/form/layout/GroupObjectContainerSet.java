package platform.interop.form.layout;

import java.awt.*;

// в этот класс вынесено автоматическое создание контейнеров при создании GroupObject
public class GroupObjectContainerSet<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

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

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> GroupObjectContainerSet<C, T> create(AbstractGroupObjectView<T> group, ContainerFactory<C> factory) {

        GroupObjectContainerSet<C, T> set = new GroupObjectContainerSet<C, T>();

        set.groupContainer = factory.createContainer(); // контейнер всей группы
        set.groupContainer.setTitle(group.getCaption());
        set.groupContainer.setDescription("Группа объектов");
        set.groupContainer.setSID("groupContainer" + group.getID());
        set.groupContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setDescription("Табличная часть");
        set.gridContainer.setSID("gridContainer" + group.getID());
        set.gridContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.groupContainer.add((T)set.gridContainer);

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setDescription("Панель");
        set.panelContainer.setSID("panelContainer" + group.getID());
        set.groupContainer.add((T)set.panelContainer);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription("Управляющие объекты");
        set.controlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.getConstraints().insetsInside = new Insets(0,0,0,0);
        set.controlsContainer.getConstraints().insetsSibling = new Insets(0,0,0,0);
        set.panelContainer.add((T)set.controlsContainer);

        set.filterContainer = factory.createContainer(); // контейнер фильтров
        set.filterContainer.setDescription("Контейнер фильтров");
        set.filterContainer.setSID("filterContainer" + group.getID());
        set.filterContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.add((T)set.filterContainer);

        group.getGrid().getConstraints().fillVertical = 1;
        group.getGrid().getConstraints().fillHorizontal = 1;

        set.gridContainer.add(group.getGrid());

        group.getShowType().getConstraints().directions = new SimplexComponentDirections(0.01,0,0,0.01);
        set.controlsContainer.add(group.getShowType());

        return set;
    }
}
