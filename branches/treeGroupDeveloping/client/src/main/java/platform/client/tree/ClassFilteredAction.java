package platform.client.tree;

import platform.client.tree.ClientTreeAction;

import javax.swing.tree.TreePath;

public abstract class ClassFilteredAction extends ClientTreeAction {
    private final Class clazz;

    public ClassFilteredAction(String name, Class clazz) {
        super(name);
        this.clazz = clazz;
    }

    @Override
    public boolean isApplicable(TreePath path) {
        return path != null && clazz != null && clazz.isInstance(path.getLastPathComponent());
    }
}
