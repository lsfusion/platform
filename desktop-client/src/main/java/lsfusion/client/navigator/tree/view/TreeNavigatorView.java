package lsfusion.client.navigator.tree.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.tree.ClientTree;
import lsfusion.client.navigator.tree.ClientTreeAction;
import lsfusion.client.navigator.tree.ClientTreeActionEvent;
import lsfusion.client.navigator.tree.ClientTreeNode;
import lsfusion.client.navigator.view.NavigatorView;
import lsfusion.client.navigator.window.ClientNavigatorWindow;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Set;

public class TreeNavigatorView extends NavigatorView {
    private ClientTreeNode root;
    private ClientTree tree;
    private ClientNavigatorElement selected;
    public DefaultTreeModel model;

    public TreeNavigatorView(ClientNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new ClientTree(), controller);
        tree = (ClientTree) getComponent();
        root = new ClientTreeNode(ClientResourceBundle.getString("navigator.root"));
        model = new DefaultTreeModel(root);
        tree.setModel(model);
        tree.setRootVisible(false);
        tree.setToggleClickCount(1);
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        root = new ClientTreeNode(ClientResourceBundle.getString("navigator.root"));
        addRootActions();
        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(root, element, newElements);
            }
        }
        model = new DefaultTreeModel(root);
        tree.setModelPreservingState(model);
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
        return selected;
    }

    private void addRootActions() {
        root.addSubTreeAction(new ClientTreeAction(ClientResourceBundle.getString("navigator.open"), true) {
            public void actionPerformed(ClientTreeActionEvent e) {
                selected = (ClientNavigatorElement) e.getNode().getUserObject();
                controller.update();
                controller.openElement(selected, 0);
            }
        });
    }

    private static class TreeNavigatorViewNode extends ClientTreeNode<ClientNavigatorElement, TreeNavigatorViewNode> {
        public TreeNavigatorViewNode(ClientNavigatorElement element) {
            super(element);
        }

        @Override
        public Icon getIcon() {
            return getTypedObject().getImage();
        }
    }
}
