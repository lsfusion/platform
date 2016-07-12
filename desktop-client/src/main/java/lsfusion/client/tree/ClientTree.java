package lsfusion.client.tree;

import lsfusion.base.BaseUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClientTree extends JTree {
    private final PopupTrigger popupTrigger = new PopupTrigger();

    // не вызываем верхний конструктор, потому что у JTree по умолчанию он на редкость дебильный
    public ClientTree() {
        super(new ClientTreeNode());
        setToggleClickCount(-1);

        addMouseListener(popupTrigger);
        addKeyListener(popupTrigger.keyListener);

        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        //setDropMode(DropMode.ON);
        //setDragEnabled(true);
        setTransferHandler(new ClientTransferHandler());

        //так делается, потому что оказывается, что все чтение UI у них в DefaultTreeCellRenderer написано в конструкторе !!!!
        setCellRenderer(new ClientTreeCellRenderer());
    }

    @Override
    public DefaultTreeModel getModel() {
        return (DefaultTreeModel) super.getModel();
    }

    public TreePath getPathToRoot(TreeNode treeNode) {
        return new TreePath(getModel().getPathToRoot(treeNode));
    }

    public void setModelPreservingState(DefaultTreeModel newModel) {
        if (treeModel == null || newModel == null) {
            setModel(newModel);
            return;
        }

        Enumeration<TreePath> paths = getExpandedDescendants(new TreePath(treeModel.getRoot()));
        final Set<TreePath> expanded = paths == null
                ? new HashSet<TreePath>()
                : new HashSet<TreePath>(Collections.list(paths));
        final TreePath selectionPath = getSelectionPath();

        setModel(newModel);
        Enumeration<DefaultMutableTreeNode> nodes = ((DefaultMutableTreeNode) treeModel.getRoot()).depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = nodes.nextElement();
            TreePath path = new TreePath(node.getPath());

            for (TreePath expandedPath : expanded) {
                if (comparePathsByUserObjects(expandedPath, path)) {
                    expandPath(path);
                }
            }

            if (comparePathsByUserObjects(selectionPath, path)) {
                getSelectionModel().setSelectionPath(path);
            }
        }
    }

    public TreePath findPathByUserObjects(Object[] path) {
        if (treeModel == null) {
            return null;
        }

        DefaultMutableTreeNode lastNode = null;
        List<DefaultMutableTreeNode> children = BaseUtils.toList((DefaultMutableTreeNode) treeModel.getRoot());
        for (Object current : path) {
            lastNode = null;
            for (DefaultMutableTreeNode child : children) {
                if (BaseUtils.nullEquals(child.getUserObject(), current)) {
                    lastNode = child;
                    children = Collections.list(child.children());
                    break;
                }
            }

            if (lastNode == null) {
                return null;
            }
        }

        return lastNode != null ? getPathToRoot(lastNode) : null;
    }

    public DefaultMutableTreeNode getSelectionNode() {
        TreePath path = getSelectionPath();
        if (path == null) {
            return null;
        }

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    class PopupTrigger extends MouseAdapter {
        public KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                TreePath path = getSelectionPath();
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (path != null) {
                        Rectangle d = getPathBounds(path);
                        executeAction(d.x + d.width, d.y + d.height, path, false);
                    }
                }
                List<ClientTreeAction> actions = getActions(path);
                int[] rows = getSelectionRows();
                for (ClientTreeAction action : actions) {
                    if (action.keyCode == e.getKeyCode()) {
                        action.actionPerformed(new ClientTreeActionEvent((ClientTreeNode) getSelectionNode(), null));
                        int row = Math.min(rows[0] + 1, getRowCount() - 2);
                        setSelectionRow(row);
                    }
                }
            }
        };

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger() || e.getClickCount() == 2) {
                int x = e.getX();
                int y = e.getY();
                final TreePath path = getPathForLocation(x, y);
                if (e.isPopupTrigger() || e.getClickCount() == 2) {
                    executeAction(x, y, path, e.isPopupTrigger());
                }
            }
        }

        private void executeAction(int x, int y, TreePath path, boolean isPopupTrigger) {
            if (path != null) {
                setSelectionPath(path);

                final ArrayList<ClientTreeAction> actions = getActions(path);
                final ClientTreeNode node = getNode(path);

                ClientTreeAction defaultAction = null;
                for (ClientTreeAction action : actions) {
                    if (action.canBeDefault(path)) {
                        defaultAction = action;
                        break;
                    }
                }
                if (isPopupTrigger || defaultAction == null) {
                    JPopupMenu popup = new JPopupMenu();
                    for (ClientTreeAction act : actions) {
                        final ClientTreeAction treeAction = act;
                        popup.add(new AbstractAction(act.caption) {
                            public void actionPerformed(ActionEvent e) {
                                treeAction.actionPerformed(new ClientTreeActionEvent(node, e));
                            }
                        });
                    }
                    if (popup.getComponentCount() > 0) {
                        popup.show(ClientTree.this, x, y);
                    }
                } else {
                    defaultAction.actionPerformed(new ClientTreeActionEvent(node));
                }
            }
        }
    }

    private ArrayList<ClientTreeAction> getActions(TreePath path) {
        ClientTreeNode node = ClientTree.getNode(path);

        ArrayList<ClientTreeAction> result = new ArrayList<ClientTreeAction>();

        if (node != null) {
            int cnt = path.getPathCount();
            for (int i = 0; i < cnt; i++) {
                Object oNode = path.getPathComponent(i);
                if (oNode instanceof ClientTreeNode) {
                    ClientTreeNode<?, ?> pathNode = (ClientTreeNode) oNode;

                    result.addAll(pathNode.subTreeActions);

                    if (i == cnt - 2) {
                        result.addAll(pathNode.sonActions);
                    }

                    if (i == cnt - 1) {
                        result.addAll(pathNode.nodeActions);
                    }
                }
            }


            for (Iterator<ClientTreeAction> it = result.iterator(); it.hasNext();) {
                ClientTreeAction act = it.next();

                if (!act.isApplicable(node)) {
                    it.remove();
                }
            }
        }

        return result;
    }

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
            return node instanceof ClientTreeNode && ((ClientTreeNode) node).canImport(info);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (canImport(info)) {
                JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
                TreePath path = dl.getPath();
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node instanceof ClientTreeNode) {
                        return ((ClientTreeNode) node).importData(ClientTree.this, info);
                    }
                }
            }
            return true;
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
        public void exportDone(JComponent component, Transferable trans, int action) {
            if (trans instanceof NodeTransfer) {
                ((NodeTransfer) trans).node.exportDone(ClientTree.this, component, trans, action);
            }
        }
    }

    private static DataFlavor CLIENTTREENODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
            "; class=lsfusion.client.tree.ClientTreeNode", "ClientTreeNode");

    public class NodeTransfer implements Transferable {
        public ClientTreeNode node;

        public NodeTransfer(ClientTreeNode node) {
            this.node = node;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{CLIENTTREENODE_FLAVOR};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CLIENTTREENODE_FLAVOR.equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return node;
        }
    }

    public static ClientTreeNode getNode(TreePath path) {
        if (path == null) {
            return null;
        }

        int cnt = path.getPathCount();
        for (int i = cnt - 1; i >= 0; i--) {
            Object node = path.getPathComponent(i);
            if (node instanceof ClientTreeNode) {
                return (ClientTreeNode) node;
            }
        }

        return null;
    }

    public static ClientTreeNode getNode(TransferHandler.TransferSupport info) {

        try {

            Object transferData = info.getTransferable().getTransferData(CLIENTTREENODE_FLAVOR);
            if (!(transferData instanceof ClientTreeNode)) {
                return null;
            }

            return (ClientTreeNode) transferData;

        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getChildIndex(TransferHandler.TransferSupport info) {
        return ((JTree.DropLocation) info.getDropLocation()).getChildIndex();
    }

    public static Object[] convertTreePathToUserObjects(TreePath path) {
        if (path == null) {
            return null;
        }

        Object[] pathElements = path.getPath();
        Object[] result = new Object[pathElements.length];
        for (int i = 0; i < pathElements.length; ++i) {
            result[i] = ((DefaultMutableTreeNode) pathElements[i]).getUserObject();
        }

        return result;
    }

    public static boolean comparePathsByUserObjects(TreePath path1, TreePath path2) {
        if (path1 == null || path2 == null || path1.getPathCount() != path2.getPathCount()) {
            return false;
        }

        while (path1 != null && path2 != null) {
            DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) path1.getLastPathComponent();
            DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) path2.getLastPathComponent();

            if (!BaseUtils.nullEquals(node1.getUserObject(), node2.getUserObject())) {
                return false;
            }

            path1 = path1.getParentPath();
            path2 = path2.getParentPath();
        }

        return true;
    }

    public static class ClientTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            Component renderer = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            ClientTreeNode node = (ClientTreeNode) value;
            if (node != null && node.getIcon() != null) {
                setIcon(node.getIcon());
            }

            return renderer;
        }
    }
}
