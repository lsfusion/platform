package platform.client.descriptor.nodes.actions;

import javax.swing.*;
import javax.swing.tree.TreePath;

public abstract class FilterAction extends AbstractAction {

    public FilterAction() {
    }


    public FilterAction(String name) {
        super(name);
    }


    public FilterAction(String name, Icon icon) {
	super(name, icon);
    }


    public boolean isApplicable(TreePath path) {
        return true;
    }
}
