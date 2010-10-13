package platform.client;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ClientTree extends JTree {


    // не вызываем верхний конструктор, потому что у JTree по умолчанию он на редкость дебильный
    public ClientTree() {
        super(new DefaultMutableTreeNode());
        setToggleClickCount(-1);
        addMouseListener(new PopupTrigger());

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    TreePath path = getSelectionPath();
                    ArrayList<Action> list = getActions(path);
                    if (list.size() > 0) {
                        list.get(0).actionPerformed(null);
                    }
                }
            }
        });

        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        //setDropMode(DropMode.ON);
        //setDragEnabled(true);
        setTransferHandler(new ClientTransferHandler());
    }

    @Override
    public DefaultTreeModel getModel() {
        return (DefaultTreeModel) super.getModel();
    }

    public void setModelPreservingExpansion(DefaultTreeModel newModel) {
        if (treeModel == null || newModel == null) {
            setModel(newModel);
            return;
        }

        Enumeration<TreePath> paths = getExpandedDescendants(new TreePath(treeModel.getRoot()));

        Set<Object> expanded = new HashSet<Object>();
        if (paths != null) {
            while (paths.hasMoreElements()) {
                Object node = ((DefaultMutableTreeNode) paths.nextElement().getLastPathComponent()).getUserObject();
                if (node != null) {
                    expanded.add(node);
                }
            }
        }

        setModel(newModel);
        expandNodes(new TreePath(treeModel.getRoot()), expanded);
    }

    private void expandNodes(TreePath parent, Set<Object> expanded) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if ( node.getChildCount() >= 0){
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandNodes(path, expanded);
            }
        }

        if (expanded.contains(((DefaultMutableTreeNode)node).getUserObject())) {
            expandPath(parent);
        }
    }

    protected void changeCurrentElement() {
    }

    protected void refreshModel() {
    }

    public DefaultMutableTreeNode getSelectionNode() {

        TreePath path = getSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    class PopupTrigger extends MouseAdapter {
        public void mouseReleased(MouseEvent e) {

            if (e.isPopupTrigger() || e.getClickCount() == 2) {
                int x = e.getX();
                int y = e.getY();
                TreePath path = getPathForLocation(x, y);
                if (path != null) {
                    setSelectionPath(path);
                    JPopupMenu popup = new JPopupMenu();
                    ArrayList<Action> list = getActions(path);
                    Action defaultAction = null;
                    if (list.size() > 0) {
                        defaultAction = list.get(0);
                    }
                    for (Action act : list) {
                        popup.add(act);
                    }

                    if (e.isPopupTrigger() && popup.getComponentCount() > 0) {
                        popup.show(ClientTree.this, x, y);
                    } else if (e.getClickCount() == 2 && defaultAction != null) {
                        defaultAction.actionPerformed(null);
                    }
                }
            }
        }
    }

    private ArrayList<Action> getActions(TreePath path) {
        if (path == null) {
            return null;
        }
        ArrayList<Action> list = new ArrayList<Action>();

        int cnt = path.getPathCount();
        for (int i = 0; i < cnt; i++) {
            Object oNode = path.getPathComponent(i);
            if (oNode instanceof ClientTreeNode) {
                ClientTreeNode node = (ClientTreeNode) oNode;

                for (Action act : node.subTreeActions) {
                    list.add(act);
                }

                if (i == cnt - 2) {
                    for (Action act : node.sonActions) {
                        list.add(act);
                    }
                }

                if (i == cnt - 1) {
                    for (Action act : node.nodeActions) {
                        list.add(act);
                    }
                }
            }
        }
        return list;
    }

    /*
        class CloseAction extends AbstractAction {
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = getSelectionNode();
                node.removeFromParent();
            }
        }
    */
    public class ClientTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }
            info.setShowDropLocation(true);

            JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
            TreePath path = dl.getPath();
            if (path == null) {
                return false;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node instanceof ClientTreeNode) {
                return ((ClientTreeNode) node).canImport(info);
            } else {
                return false;
            }
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!canImport(info)) {
                return false;
            }

            JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
            TreePath path = dl.getPath();
            if (path == null) {
                return false;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node instanceof ClientTreeNode) {
                if (((ClientTreeNode) node).importData(ClientTree.this, info)) {
                    refreshModel();
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        public Transferable createTransferable(JComponent c) {
            DefaultMutableTreeNode node = getSelectionNode();
            if (node instanceof ClientTreeNode) {
                return new NodeTransfer((ClientTreeNode) node);
            } else {
                return null;
            }
        }

        @Override
        public void exportDone(JComponent component, Transferable trans, int mode) {
            if (trans instanceof NodeTransfer) {
                ((NodeTransfer) trans).node.exportDone(component, mode);
            }
        }
    }

    private static DataFlavor CLIENTTREENODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                  "; class=platform.client.ClientTreeNode", "ClientTreeNode");

    public class NodeTransfer implements Transferable {
        public ClientTreeNode node;

        public NodeTransfer(ClientTreeNode node) {
            this.node = node;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {CLIENTTREENODE_FLAVOR};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CLIENTTREENODE_FLAVOR.equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return node;
        }
    }

    public static ClientTreeNode getNode(TransferHandler.TransferSupport info) {

        try {

            Object transferData = info.getTransferable().getTransferData(CLIENTTREENODE_FLAVOR);
            if (!(transferData instanceof ClientTreeNode))
                return null;

            return (ClientTreeNode)transferData;
            
        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
