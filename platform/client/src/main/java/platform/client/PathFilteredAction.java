package platform.client;

import javax.swing.*;
import javax.swing.tree.TreePath;

public abstract class PathFilteredAction extends AbstractAction {

    public PathFilteredAction() {
    }


    public PathFilteredAction(String name) {
        super(name);
    }


    public PathFilteredAction(String name, Icon icon) {
	super(name, icon);
    }


    public boolean isApplicable(TreePath path) {
        return true;
    }
}
