package platform.client;

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
        if (path == null || clazz == null) {
            return false;
        }

        Object node = path.getLastPathComponent();
        return node != null && clazz.isAssignableFrom(node.getClass());
    }
}
