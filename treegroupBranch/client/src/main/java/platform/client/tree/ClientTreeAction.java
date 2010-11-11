package platform.client.tree;

import javax.swing.tree.TreePath;

public abstract class ClientTreeAction {

    public String caption;

    public ClientTreeAction(String caption) {
        this.caption = caption;
    }

    public abstract void actionPerformed(ClientTreeActionEvent e);

    public boolean isApplicable(TreePath path) {
        return true;
    }

    public boolean canBeDefault(TreePath path) {
        return true;
    }
}
