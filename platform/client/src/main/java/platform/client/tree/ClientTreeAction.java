package platform.client.tree;

import javax.swing.tree.TreePath;

public abstract class ClientTreeAction {

    public final String caption;
    public final int keyCode;
    public final boolean canBeDefault;


    public ClientTreeAction(String caption) {
        this(caption, false);
    }

    public ClientTreeAction(String caption, boolean canBeDefault) {
        this(caption, -1, canBeDefault);
    }

    public ClientTreeAction(String caption, int keyCode) {
        this(caption, keyCode, false);
    }

    public ClientTreeAction(String caption, int keyCode, boolean canBeDefault) {
        this.canBeDefault = canBeDefault;
        this.caption = caption;
        this.keyCode = keyCode;
    }

    public abstract void actionPerformed(ClientTreeActionEvent e);

    public boolean isApplicable(ClientTreeNode node) {
        assert node != null;
        return true;
    }

    public boolean canBeDefault(TreePath path) {
        return canBeDefault;
    }
}
