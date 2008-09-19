package platformlocal;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;

abstract class AbstractNavigator extends JPanel {

    // Icons - загружаем один раз, для экономии
    private final ImageIcon formIcon = new ImageIcon(getClass().getResource("images/form.gif"));
    private final ImageIcon reportIcon = new ImageIcon(getClass().getResource("images/report.gif"));

    RemoteNavigator remoteNavigator;

    NavigatorTree tree;

    public AbstractNavigator(RemoteNavigator iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new NavigatorTree();
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

                public void treeCollapsed(TreeExpansionEvent event) {};

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
            List<ClientNavigatorElement> elements = getNodeElements(elementID);

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

    abstract protected List<ClientNavigatorElement> getNodeElements(int elementID);

}

public abstract class ClientNavigator extends AbstractNavigator {

    RelevantFormNavigator relevantFormNavigator;
    RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigator iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    protected List<ClientNavigatorElement> getNodeElements(int elementID) {
        return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray(elementID));
    }

    public void changeCurrentForm(int formID) {
        if (remoteNavigator.changeCurrentForm(formID))
            relevantFormNavigator.tree.createRootNode();
    }

    public void changeCurrentClass(int classID) {
        if (remoteNavigator.changeCurrentClass(classID))
            relevantClassNavigator.tree.createRootNode();
    }

    class RelevantFormNavigator extends AbstractNavigator {

        public RelevantFormNavigator(RemoteNavigator iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            ClientNavigator.this.openForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray((elementID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTFORM : elementID));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigator iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            ClientNavigator.this.openForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray((elementID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTCLASS : elementID));
        }

    }

}

class ClientNavigatorElement {

    int ID;
    String caption;

    boolean hasChilds = false;
    boolean allowChildren() { return hasChilds; };

    public boolean isPrintForm = false;

    public String toString() { return caption; }

}

class ClientNavigatorForm extends ClientNavigatorElement {
}