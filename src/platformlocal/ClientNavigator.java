package platformlocal;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.*;
import java.util.List;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

public class ClientNavigator extends JPanel {

    RemoteNavigator remoteNavigator;

    JTree tree;

    public ClientNavigator(RemoteNavigator iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        tree = new NavigatorTree();
        JScrollPane pane = new JScrollPane(tree);
        add(pane);

    }

    private void openForm(NavigatorForm element) {
        System.out.println("Open Form");
        try {
            Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, remoteNavigator));
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    class NavigatorTree extends JTree {

        DefaultMutableTreeNode rootNode;
        DefaultTreeModel model;

        public NavigatorTree() {

            rootNode = new DefaultMutableTreeNode(null);
            model = new DefaultTreeModel(rootNode);

            setModel(model);

            addTreeExpansionListener(new TreeExpansionListener() {

                public void treeExpanded(TreeExpansionEvent event) {
                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                }

                public void treeCollapsed(TreeExpansionEvent event) {};

            });

            rootNode.add(new ExpandingTreeNode());
            expandPath(new TreePath(rootNode));

            addMouseListener(new MouseAdapter() {

                public void mouseReleased(MouseEvent e) {
                    if (e.getClickCount() == 2) {

                        TreePath path = getSelectionPath();
                        if (path == null) return;

                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node == null) return;

                        Object nodeObject = node.getUserObject();
                        if (! (nodeObject instanceof NavigatorForm)) return;

                        openForm((NavigatorForm) nodeObject);
                    }
                }
            });

        }

        private void addNodeElements(DefaultMutableTreeNode parent) {

            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

            if (! (firstChild instanceof ExpandingTreeNode)) return;
            parent.removeAllChildren();

            Object nodeObject = parent.getUserObject();
            if (nodeObject != null && ! (nodeObject instanceof NavigatorGroup) ) return;

            NavigatorGroup group = (NavigatorGroup) nodeObject;

            List<NavigatorElement> elements = remoteNavigator.GetElements(group);

            for (NavigatorElement element : elements) {

                DefaultMutableTreeNode node;
                node = new DefaultMutableTreeNode(element, element.allowChildren());
                parent.add(node);

                if (element.allowChildren())
                    node.add(new ExpandingTreeNode());

            }

            model.reload(parent);

        }

        class ExpandingTreeNode extends DefaultMutableTreeNode {

            public String toString() { return "Retreiving data..."; }
        }

    }

}
