package platform.client.navigator;

import platform.client.Main;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeAction;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ExpandingTreeNode;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class NavigatorTree extends ClientTree {

    public NavigatorTreeNode rootNode;
    public final DefaultTreeModel model;
    public AbstractNavigator navigator;

    public NavigatorTree(AbstractNavigator navigator) {
        super();
        this.navigator = navigator;

        setToggleClickCount(-1);

        model = new DefaultTreeModel(null);

        setModel(model);

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                try {
                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при получении информации о формах", e);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException { }
        });

        createRootNode();
    }

    public void createRootNode() {
        rootNode = new NavigatorTreeNode(this, new ClientNavigatorElement(0, AbstractNavigator.BASE_ELEMENT_SID, "", true));
        model.setRoot(rootNode);

        rootNode.add(new ExpandingTreeNode());
        expandPath(new TreePath(rootNode));

        rootNode.addSubTreeAction(new ClientTreeAction("Открыть"){
            public void actionPerformed(ClientTreeActionEvent e) {
                changeCurrentElement();
            }

            @Override
            public boolean isApplicable(TreePath path) {
                if (path == null) return false;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node == null) return false;

                Object nodeObject = node.getUserObject();
                return nodeObject instanceof ClientNavigatorForm;
            }

            @Override
            public boolean canBeDefault(TreePath path) {
                return true;
            }
        });
    }

    //@Override
    protected void changeCurrentElement() {

        TreePath path = getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node == null) return;

        Object nodeObject = node.getUserObject();
        if (! (nodeObject instanceof ClientNavigatorForm)) return;

        try {
            navigator.openForm((ClientNavigatorForm) nodeObject);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при открытии формы", e);
        }
    }

    private void addNodeElements(DefaultMutableTreeNode parent) throws IOException {

        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

        if (! (firstChild instanceof ExpandingTreeNode)) return;
        parent.removeAllChildren();

        Object nodeObject = parent.getUserObject();
        if (! (nodeObject instanceof ClientNavigatorElement) ) return;

        ClientNavigatorElement element = (ClientNavigatorElement) nodeObject;

        if (!element.hasChildren()) return;

        java.util.List<ClientNavigatorElement> elements = navigator.getNodeElements(element.getSID());

        for (ClientNavigatorElement child : elements) {

            DefaultMutableTreeNode node;
            node = new NavigatorTreeNode(this, child);
            parent.add(node);

            if (child.hasChildren()) {
                node.add(new ExpandingTreeNode());
            }
        }

        model.reload(parent);
    }
}
