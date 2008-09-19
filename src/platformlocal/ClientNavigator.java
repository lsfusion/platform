package platformlocal;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;

abstract class AbstractNavigator extends JPanel {

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

                        TreePath path = getSelectionPath();
                        if (path == null) return;

                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node == null) return;

                        Object nodeObject = node.getUserObject();
                        if (! (nodeObject instanceof ClientNavigatorForm)) return;

                        openForm((ClientNavigatorForm) nodeObject);
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

        private void addNodeElements(DefaultMutableTreeNode parent) {

            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

            if (! (firstChild instanceof ExpandingTreeNode)) return;
            parent.removeAllChildren();

            Object nodeObject = parent.getUserObject();
            if (nodeObject != null && ! (nodeObject instanceof ClientNavigatorGroup) ) return;

            ClientNavigatorGroup group = (ClientNavigatorGroup) nodeObject;

            int groupID = (group == null) ? -1 : group.ID;
            List<ClientNavigatorElement> elements = getNodeElements(groupID);

            for (ClientNavigatorElement element : elements) {

                DefaultMutableTreeNode node;
                node = new DefaultMutableTreeNode(element, element.allowChildren());
                parent.add(node);

                if (element.allowChildren())
                    node.add(new ExpandingTreeNode());

            }

            model.reload(parent);

        }

    }

    abstract protected List<ClientNavigatorElement> getNodeElements(int groupID);

}

public abstract class ClientNavigator extends AbstractNavigator {

    RelevantFormNavigator relevantFormNavigator;
    RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigator iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    protected List<ClientNavigatorElement> getNodeElements(int groupID) {
        return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray(groupID));
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

        protected List<ClientNavigatorElement> getNodeElements(int groupID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray((groupID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTFORM : groupID));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigator iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            ClientNavigator.this.openForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int groupID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.GetElementsByteArray((groupID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTCLASS : groupID));
        }

    }

}

abstract class ClientNavigatorElement {

    int ID;
    String caption;

    abstract boolean allowChildren();

    public String toString() { return caption; }

}

class ClientNavigatorGroup extends ClientNavigatorElement {


    boolean allowChildren() {
        return true;
    }
}

class ClientNavigatorForm extends ClientNavigatorElement {

    boolean allowChildren() {
        return false;
    }
}