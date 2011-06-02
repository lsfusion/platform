package platform.interop.form.layout;

public class TreeGroupContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C container;
    public C getContainer() {
        return container;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> TreeGroupContainerSet<C, T> create(AbstractTreeGroup<C,T> treeView, ContainerFactory<C> factory) {

        TreeGroupContainerSet<C,T> set = new TreeGroupContainerSet<C,T>();

        set.container = factory.createContainer();
        set.container.setTitle("Дерево");
        set.container.setDescription("Дерево");
        set.container.setSID(GroupObjectContainerSet.TREE_GROUP_CONTAINER + treeView.getID());
        set.container.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
        set.container.add(treeView.getComponent());

        treeView.getConstraints().fillVertical = 1;
        treeView.getConstraints().fillHorizontal = 1;

        return set;
    }

}
