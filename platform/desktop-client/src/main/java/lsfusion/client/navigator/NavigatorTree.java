package lsfusion.client.navigator;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.tree.*;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.List;

public class NavigatorTree extends ClientTree {

    public NavigatorTreeNode rootNode;
    public final DefaultTreeModel model;
    public final AbstractNavigatorPanel navigator;
    private final ClientNavigatorElement rootElement;

    public NavigatorTree(AbstractNavigatorPanel navigator, ClientNavigatorElement rootElement) {
        super();
        this.navigator = navigator;
        this.rootElement = rootElement;

        setToggleClickCount(-1);

        model = new DefaultTreeModel(null);

        setModel(model);

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                try {
                    addNodeElements(ClientTree.getNode(event.getPath()));
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.info.about.forms"), e);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException { }
        });

        createRootNode();
    }

    public void createRootNode() {
        rootNode = new NavigatorTreeNode(this, rootElement);
        model.setRoot(rootNode);

        rootNode.add(new ExpandingTreeNode());
        expandPath(new TreePath(rootNode));

        rootNode.addSubTreeAction(new ClientTreeAction(ClientResourceBundle.getString("navigator.open"), true) {
            public void actionPerformed(ClientTreeActionEvent e) {
                openForm((ClientNavigatorForm) e.getNode().getUserObject());
            }

            @Override
            public boolean isApplicable(ClientTreeNode node) {
                return node.getUserObject() instanceof ClientNavigatorForm;
            }
        });

        rootNode.addSubTreeAction(new ClientTreeAction(ClientResourceBundle.getString("navigator.execute.action"), true) {
            public void actionPerformed(ClientTreeActionEvent e) {
                openAction((ClientNavigatorAction) e.getNode().getUserObject());
            }

            @Override
            public boolean isApplicable(ClientTreeNode node) {
                return node.getUserObject() instanceof ClientNavigatorAction;
            }
        });
    }

    private void openAction(ClientNavigatorAction action) {
        navigator.openAction(action);
    }

    private void openForm(ClientNavigatorForm form) {
        assert form != null;
        try {
            navigator.openForm(form);
        } catch (Exception e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.opening.form"), e);
        }
    }

    private void addNodeElements(ClientTreeNode parent) throws IOException {

        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) parent.getFirstChild();

        if (!(firstChild instanceof ExpandingTreeNode)) return;
        parent.removeAllChildren();

        Object nodeObject = parent.getUserObject();
        if (!(nodeObject instanceof ClientNavigatorElement)) return;

        ClientNavigatorElement element = (ClientNavigatorElement) nodeObject;

        if (!element.hasChildren()) return;

        List<ClientNavigatorElement> elements = navigator.getNodeElements(element);

        for (ClientNavigatorElement child : elements) {
            NavigatorTreeNode node = new NavigatorTreeNode(this, child);
            parent.add(node);

            if (child.hasChildren()) {
                node.add(new ExpandingTreeNode());
            }
        }

        model.reload(parent);
    }
}
