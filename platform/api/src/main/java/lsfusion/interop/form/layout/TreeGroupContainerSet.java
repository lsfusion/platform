package lsfusion.interop.form.layout;

import java.awt.*;

import static lsfusion.base.ApiResourceBundle.getString;
import static lsfusion.interop.form.layout.GroupObjectContainerSet.*;

public class TreeGroupContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C treeContainer;
    private C controlsContainer;
    private C rightControlsContainer;
    private C filtersContainer;
    private C toolbarPropsContainer;

    private C toolbarContainer;
    private C filterContainer;

    public C getTreeContainer() {
        return treeContainer;
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

    public C getToolbarContainer() {
        return toolbarContainer;
    }

    public C getFilterContainer() {
        return filterContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> TreeGroupContainerSet<C, T> create(AbstractTreeGroup<C,T> treeGroup, ContainerFactory<C> factory) {
        TreeGroupContainerSet<C,T> set = new TreeGroupContainerSet<C,T>();

        set.treeContainer = factory.createContainer();
        set.treeContainer.setTitle(getString("form.layout.tree"));
        set.treeContainer.setDescription(getString("form.layout.tree"));
        set.treeContainer.setSID(treeGroup.getSID() + GroupObjectContainerSet.TREE_GROUP_CONTAINER);
        set.treeContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(getString("form.layout.control.objects"));
        set.controlsContainer.setSID(treeGroup.getSID() + CONTROLS_CONTAINER);
        set.controlsContainer.getConstraints().insetsInside = new Insets(2, 2, 2, 2);
        set.controlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.getConstraints().fillHorizontal = 1.0;

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(getString("form.layout.toolbar.props.container"));
        set.toolbarPropsContainer.setSID(treeGroup.getSID() + TOOLBAR_PROPS_CONTAINER);
        set.toolbarPropsContainer.getConstraints().insetsInside = new Insets(0, 2, 2, 2);
        set.toolbarPropsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.toolbarPropsContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(getString("form.layout.filters.container"));
        set.filtersContainer.setSID(treeGroup.getSID() + FILTERS_CONTAINER);
        set.filtersContainer.getConstraints().insetsInside = new Insets(0, 2, 1, 2);
        set.filtersContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.filtersContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(treeGroup.getSID() + CONTROLS_RIGHT_CONTAINER);
        set.rightControlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.rightControlsContainer.getConstraints().insetsInside = new Insets(0, 0, 0, 0);
        set.rightControlsContainer.getConstraints().directions = new SimplexComponentDirections(0.0, -0.01, 0.0, 0.01);

        set.filterContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.filterContainer.setDescription(getString("form.layout.filter.container"));
        set.filterContainer.setSID(treeGroup.getSID() + FILTER_CONTAINER);
        set.filterContainer.getConstraints().insetsInside = new Insets(0, 1, 2, 1);
        set.filterContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        set.toolbarContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarContainer.setDescription(getString("form.layout.toolbar.container"));
        set.toolbarContainer.setSID(treeGroup.getSID() + TOOLBAR_CONTAINER);
        set.toolbarContainer.getConstraints().insetsInside = new Insets(1, 0, 2, 2);
        set.toolbarContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.toolbarContainer.getConstraints().directions = new SimplexComponentDirections(0.01, 0.01, 0.0, 0.0);

        set.toolbarContainer.add(treeGroup.getToolbar());

        set.rightControlsContainer.add((T) set.filtersContainer);
        set.rightControlsContainer.add((T) set.toolbarPropsContainer);

        set.controlsContainer.add((T) set.toolbarContainer);
        set.controlsContainer.add((T) set.rightControlsContainer);

        set.filterContainer.add(treeGroup.getFilter());

        set.treeContainer.add((T) treeGroup);
        set.treeContainer.add((T) set.controlsContainer);
        set.treeContainer.add((T) set.filterContainer);

        return set;
    }
}
