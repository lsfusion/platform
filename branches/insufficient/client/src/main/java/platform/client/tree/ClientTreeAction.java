package platform.client.tree;

import javax.swing.tree.TreePath;

public abstract class ClientTreeAction {

    public String caption;
    public int keyCode = -1;


    public ClientTreeAction(String caption) {
        this.caption = caption;
    }

    public ClientTreeAction(String caption, int keyCode) {
        this.caption = caption;
        this.keyCode = keyCode;
    }

    public abstract void actionPerformed(ClientTreeActionEvent e);

    public boolean isApplicable(TreePath path) {
        return true;
    }

    public boolean canBeDefault(TreePath path) {
        return false;
    }
}
