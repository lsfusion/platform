package platform.client.navigator;

import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeAction;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;
import platform.client.tree.ExpandingTreeNode;
import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;

public abstract class AbstractNavigator extends JPanel {

    // Icons - загружаем один раз, для экономии
    private final ImageIcon formIcon = new ImageIcon(getClass().getResource("/platform/navigator/images/form.gif"));
    private final ImageIcon reportIcon = new ImageIcon(getClass().getResource("/platform/navigator/images/report.gif"));

    public final RemoteNavigatorInterface remoteNavigator;

    final AbstractNavigator.NavigatorTree tree;

    public AbstractNavigator(RemoteNavigatorInterface iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new AbstractNavigator.NavigatorTree();
        JScrollPane pane = new JScrollPane(tree);
        add(pane);

    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;

    class NavigatorTree extends ClientTree {

        ClientTreeNode rootNode;
        final DefaultTreeModel model;

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

            rootNode = new ClientTreeNode(null);
            model.setRoot(rootNode);

            rootNode.add(new ExpandingTreeNode());
            expandPath(new TreePath(rootNode));

            rootNode.addSubTreeAction(new ClientTreeAction("Открыть"){
                public void actionPerformed(ClientTreeActionEvent e) {
                    changeCurrentElement();
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
                node = new DefaultMutableTreeNode(child, child.allowChildren());
                parent.add(node);

                if (child.allowChildren())
                    node.add(new ExpandingTreeNode());

            }

            model.reload(parent);

        }

        public void updateUI() {
            super.updateUI();

            //так делается, потому что оказывается, что все чтение UI у них в DefaultTreeCellRenderer написано в конструкторе !!!!
            setCellRenderer(new DefaultTreeCellRenderer() {

                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                              boolean expanded, boolean leaf, int row,
                                                              boolean hasFocus) {

                    Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    if (node != null) {

                        Object nodeObject = node.getUserObject();
                        if (nodeObject != null && nodeObject instanceof ClientNavigatorForm) {

                            ClientNavigatorForm form = (ClientNavigatorForm) nodeObject;
                            setIcon(form.isPrintForm ? reportIcon : formIcon);
                        }

                    }

                    return comp;

                }

            });
        }
    }

    protected java.util.List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray(elementID));
    }

}
