package platform.client;

import platform.base.BaseUtils;
import platform.client.descriptor.nodes.actions.FilterAction;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

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

    static abstract class NodeProcessor {
        public abstract void process(TreePath path);
    }

    private void traverseNodes(TreePath parent, NodeProcessor nodeProcessor) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if ( node.getChildCount() >= 0){
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                traverseNodes(path, nodeProcessor);
            }
        }

        nodeProcessor.process(parent);
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
        traverseNodes(new TreePath(treeModel.getRoot()), new NodeProcessor() {
            @Override
            public void process(TreePath path) {
                for (TreePath expandedPath : expanded) {
                    if (comparePathsByUserObjects(expandedPath, path)) {
                        expandPath(path);
                    }
                }

                if (comparePathsByUserObjects(selectionPath, path)) {
                    getSelectionModel().setSelectionPath(path);
                }
            }
        });
    }

    private boolean comparePathsByUserObjects(TreePath path1, TreePath path2) {
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

    protected void changeCurrentElement() {
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
                ClientTreeNode<?,?> node = (ClientTreeNode) oNode;

                list.addAll(node.subTreeActions);

                if (i == cnt - 2) {
                    list.addAll(node.sonActions);
                }

                if (i == cnt - 1) {
                    list.addAll(node.nodeActions);
                }
            }
        }

        for (Iterator<Action> it = list.iterator(); it.hasNext(); ) {
            Action act = it.next();
            if (act instanceof FilterAction) {
                FilterAction filterAction = (FilterAction) act;
                if (!filterAction.isApplicable(path)) {
                    it.remove();
                }
            }
        }

        return list;
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
                    if (node instanceof ClientTreeNode)
                        return ((ClientTreeNode) node).importData(ClientTree.this, info);
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
