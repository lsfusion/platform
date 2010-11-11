package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.client.tree.*;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;

public abstract class AbstractNavigator extends JPanel {
    public final RemoteNavigatorInterface remoteNavigator;

    protected final AbstractNavigator.NavigatorTree tree;

    public AbstractNavigator(RemoteNavigatorInterface iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new AbstractNavigator.NavigatorTree();
        JScrollPane pane = new JScrollPane(tree);
        add(pane);

    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;

    protected class NavigatorTree extends ClientTree {

        public ClientTreeNode rootNode;
        public final DefaultTreeModel model;

        public NavigatorTree() {
            super();

            setToggleClickCount(-1);

            model = new DefaultTreeModel(null);

            setModel(model);

            addTreeExpansionListener(new TreeExpansionListener() {

                public void treeExpanded(TreeExpansionEvent event) {
                    try {
                        addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при получении информации о формах", e);
                    }
                }

                public void treeCollapsed(TreeExpansionEvent event) {}

            });

            createRootNode();

        }

        public void createRootNode() {

            rootNode = new NavigatorTreeNode();
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
                openForm((ClientNavigatorForm) nodeObject);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при открытии формы", e);
            }

        }

        private void addNodeElements(DefaultMutableTreeNode parent) throws IOException {

            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

            if (! (firstChild instanceof ExpandingTreeNode)) return;
            parent.removeAllChildren();

            Object nodeObject = parent.getUserObject();
            if (nodeObject != null && ! (nodeObject instanceof ClientNavigatorElement) ) return;

            ClientNavigatorElement element = (ClientNavigatorElement) nodeObject;

            if (element != null && !element.allowChildren()) return;

            int elementID = (element == null) ? -1 : element.ID;
            java.util.List<ClientNavigatorElement> elements = getNodeElements(elementID);

            for (ClientNavigatorElement child : elements) {

                DefaultMutableTreeNode node;
                node = new NavigatorTreeNode(child, child.allowChildren());
                parent.add(node);

                if (child.allowChildren())
                    node.add(new ExpandingTreeNode());

            }

            model.reload(parent);
        }
    }

    protected java.util.List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray(elementID));
    }
}
