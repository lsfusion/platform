package platform.client.navigator;

import platform.client.Main;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;

public class NavigatorTreeNode extends ClientTreeNode<ClientNavigatorElement, NavigatorTreeNode> {
    private static final ImageIcon formIcon = new ImageIcon(Main.class.getResource("/platform/navigator/images/form.gif"));
    private static final ImageIcon reportIcon = new ImageIcon(Main.class.getResource("/platform/navigator/images/report.gif"));

    private final ClientNavigatorElement navigatorElement;

    public NavigatorTreeNode() {
        this(null, true);
    }

    public NavigatorTreeNode(ClientNavigatorElement navigatorElement, boolean allowsChildren) {
        super(navigatorElement, allowsChildren);

        this.navigatorElement = navigatorElement;
    }

    public boolean canImport(TransferHandler.TransferSupport info) {
        ClientTreeNode node = ClientTree.getNode(info);
        return node instanceof NavigatorTreeNode && !node.isNodeDescendant(this) && this.allowsChildren;
    }

    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        tree.expandPath(tree.getPathToRoot(this));

        NavigatorTreeNode draggingNode = ((NavigatorTreeNode) ClientTree.getNode(info));
        NavigatorTreeNode parentNode = draggingNode.getParent();

        int index = ClientTree.getChildIndex(info);
        if (index == -1) {
            index = getChildCount();
        }

        if (parentNode == this) {
            int origIndex = parentNode.getIndex(draggingNode);
            parentNode.remove(draggingNode);
            parentNode.insert(draggingNode, index > origIndex ? index - 1 : index);
        } else {
            parentNode.remove(draggingNode);
            insert(draggingNode, index);

            tree.getModel().reload(parentNode);
        }

        tree.getModel().reload(this);

        return true;
    }

    @Override
    public Icon getIcon() {
        if (navigatorElement instanceof ClientNavigatorForm) {
            return ((ClientNavigatorForm) navigatorElement).isPrintForm ? reportIcon : formIcon;
        }
        return null;
    }

    @Override
    public NavigatorTreeNode getParent() {
        return (NavigatorTreeNode) super.getParent();
    }
}
