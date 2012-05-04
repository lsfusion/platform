package platform.client.tree;

public abstract class ClassFilteredAction extends ClientTreeAction {
    private final Class clazz;

    public ClassFilteredAction(String name, Class clazz) {
        this(name, clazz, false);
    }

    public ClassFilteredAction(String name, Class clazz, boolean canBeDefault) {
        super(name);

        if (clazz == null) {
            throw new IllegalArgumentException("Class can't be null!");
        }

        this.clazz = clazz;
    }

    @Override
    public boolean isApplicable(ClientTreeNode node) {
        return clazz.isInstance(node);
    }
}
