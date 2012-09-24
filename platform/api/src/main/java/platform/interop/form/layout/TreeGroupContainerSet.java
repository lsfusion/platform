package platform.interop.form.layout;

import java.awt.*;

import static platform.base.ApiResourceBundle.getString;

public class TreeGroupContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C treeContainer;
    private C controlsContainer;

    public C getTreeContainer() {
        return treeContainer;
    }

    public C getControlsContainer() {
        return controlsContainer;
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
        set.controlsContainer.setSID(treeGroup.getSID() + GroupObjectContainerSet.CONTROLS_CONTAINER);
        set.controlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.controlsContainer.getConstraints().insetsInside = new Insets(0, 0, 0, 0);
        set.controlsContainer.getConstraints().insetsSibling = new Insets(0, 0, 0, 0);

        treeGroup.getToolbar().getConstraints().directions = new SimplexComponentDirections(0.01, 0.01, 0, 0);

        treeGroup.getConstraints().fillVertical = 1;
        treeGroup.getConstraints().fillHorizontal = 1;

        set.controlsContainer.add(treeGroup.getToolbar());

        set.treeContainer.add((T) treeGroup);
        set.treeContainer.add((T) set.controlsContainer);
        set.treeContainer.add(treeGroup.getFilter());

        return set;
    }
}
