package platform.client.navigator;

import platform.server.view.navigator.RemoteNavigator;
import platform.client.ExpandingTreeNode;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public abstract class AbstractNavigator extends JPanel {

    // Icons - загружаем один раз, для экономии
    private final ImageIcon formIcon = new ImageIcon(getClass().getResource("/platform/client/navigator/images/form.gif"));
    private final ImageIcon reportIcon = new ImageIcon(getClass().getResource("/platform/client/navigator/images/report.gif"));

    public RemoteNavigator remoteNavigator;

    AbstractNavigator.NavigatorTree tree;

    public AbstractNavigator(RemoteNavigator iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new AbstractNavigator.NavigatorTree();
        JScrollPane pane = new JScrollPane(tree);
        add(pane);

    }

    public abstract void openForm(ClientNavigatorForm element);

    class NavigatorTree extends JTree {

        DefaultMutableTreeNode rootNode;
        DefaultTreeModel model;

        public NavigatorTree() {

            setToggleClickCount(-1);

            model = new DefaultTreeModel(null);

            setModel(model);

            addTreeExpansionListener(new TreeExpansionListener() {

                public void treeExpanded(TreeExpansionEvent event) {
                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                }

                public void treeCollapsed(TreeExpansionEvent event) {}

            });

            addMouseListener(new MouseAdapter() {

                public void mouseReleased(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        changeCurrentElement();
                    }
                }

            });

            addKeyListener(new KeyAdapter() {

                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        changeCurrentElement();
                    }
                }
            });

            createRootNode();

        }

        public void createRootNode() {

            rootNode = new DefaultMutableTreeNode(null);
            model.setRoot(rootNode);

            rootNode.add(new ExpandingTreeNode());
            expandPath(new TreePath(rootNode));
        }

        private void changeCurrentElement() {

            TreePath path = getSelectionPath();
            if (path == null) return;

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null) return;

            Object nodeObject = node.getUserObject();
            if (! (nodeObject instanceof ClientNavigatorForm)) return;

            openForm((ClientNavigatorForm) nodeObject);

        }

        private void addNodeElements(DefaultMutableTreeNode parent) {

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

    abstract protected java.util.List<ClientNavigatorElement> getNodeElements(int elementID);

}
