package platform.client.navigator;

import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeAction;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

public class TreeNavigatorView extends NavigatorView {
    private ClientTreeNode root;
    private ClientTree tree;
    private TreePath selectedPath;
    public DefaultTreeModel model;

    public TreeNavigatorView(ClientNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new ClientTree(), controller);
        tree = (ClientTree) component;
        root = new ClientTreeNode("Корень");
        model = new DefaultTreeModel(root);
        tree.setModel(model);
        tree.setRootVisible(false);
        tree.setToggleClickCount(1);
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        root = new ClientTreeNode("Корень");
        addRootActions();
        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(root, element, newElements);
            }
        }
        model = new DefaultTreeModel(root);
        tree.setModelPreservingState(model);
        if (window.drawRoot)
            tree.expandRow(0);
    }

    private void addElement(DefaultMutableTreeNode parent, ClientNavigatorElement element, Set<ClientNavigatorElement> newElements) {
        DefaultMutableTreeNode node = addNode(parent, element);
        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (ClientNavigatorElement childEl : element.children) {
            if (newElements.contains(childEl)) {
                addElement(node, childEl, newElements);
            }
        }
    }


    private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, ClientNavigatorElement element) {
        DefaultMutableTreeNode node = new TreeNavigatorViewNode(element);
        parent.add(node);
        return node;
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        if (selectedPath == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        return (ClientNavigatorElement) node.getUserObject();
    }

    private void addRootActions() {
        root.addSubTreeAction(new ClientTreeAction("Открыть") {
            public void actionPerformed(ClientTreeActionEvent e) {
                selectedPath = tree.getSelectionPath();
                if (selectedPath == null) return;
                controller.update();
                controller.openForm(getSelectedElement());
            }

            @Override
            public boolean canBeDefault(TreePath path) {
                return true;
            }
        });

    }
}

class TreeNavigatorViewNode extends ClientTreeNode {
    ClientNavigatorElement element;

    public TreeNavigatorViewNode(ClientNavigatorElement element) {
        super(element);
        this.element = element;
    }

    @Override
    public Icon getIcon() {
        return element.image;
    }

}