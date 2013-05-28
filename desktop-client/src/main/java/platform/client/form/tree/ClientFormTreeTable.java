package platform.client.form.tree;

import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;
import platform.client.SwingUtils;
import platform.client.form.TableTransferHandler;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Position;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

public abstract class ClientFormTreeTable extends JXTreeTable implements TableTransferHandler.TableInterface {

    protected ClientFormTreeTable() {

        SwingUtils.setupClientTable(this);

        setToggleClickCount(-1);

        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        setShowGrid(true, true);

        setupActionMap();
    }

    private void setupActionMap() {
        //  Have the enter key work the same as the tab key
        InputMap im = getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStrokes.getEnter(), im.get(KeyStrokes.getTab()));

        am.put("selectNextColumn", new ExpandAction(true, am.get("selectNextColumn")));
        am.put("selectPreviousColumn", new ExpandAction(false, am.get("selectPreviousColumn")));
    }

    /**
     * теоретически может веруть null
     */
    protected JXTree getHierarhicalColumnRenderer() {
        TableCellRenderer cellRenderer = getCellRenderer(0, getHierarchicalColumn());
        return cellRenderer instanceof JXTree
               ? (JXTree) cellRenderer
               : null;
    }

    /**
     * @see javax.swing.JTree#getNextMatch
     */
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias forward) {
        JXTree tree = getHierarhicalColumnRenderer();
        return tree != null
               ? tree.getNextMatch(prefix, startingRow, forward)
               : null;
    }

    public interface NodeProccessor {
        public void processPath(TreePath nodePath);
    }

    public void enumerateNodesDepthFirst(NodeProccessor nodeProccessor) {
        TreeTableModel treeTableModel = getTreeTableModel();
        if (nodeProccessor != null && treeTableModel != null && treeTableModel.getRoot() != null) {
            enumerateNodes(new TreePath(treeTableModel.getRoot()), nodeProccessor);
        }
    }

    private void enumerateNodes(TreePath currentPath, NodeProccessor nodeProccessor) {
        TreeNode parentNode = (TreeNode) currentPath.getLastPathComponent();
        if (parentNode.getChildCount() >= 0) {
            Enumeration<MutableTreeTableNode> en = parentNode.children();
            while (en.hasMoreElements()) {
                enumerateNodes(currentPath.pathByAddingChild(en.nextElement()), nodeProccessor);
            }
        }

        nodeProccessor.processPath(currentPath);
    }

    protected class ExpandAction extends AbstractAction {
        private boolean expand;
        private Action origAction;

        public ExpandAction(boolean expand, Action orig) {
            this.expand = expand;
            this.origAction = orig;
        }

        public void actionPerformed(ActionEvent e) {
            if (getSelectedRowCount() == 1 && isHierarchical(getSelectedColumn())) {
                TreePath selPath = getPathForRow(convertRowIndexToModel(getSelectedRow()));
                if (selPath != null && !getTreeTableModel().isLeaf(selPath.getLastPathComponent())) {
                    boolean expanded = isExpanded(selPath);
                    if (expanded && !expand) {
                        collapsePath(selPath);
                    } else if (!expanded && expand) {
                        expandPath(selPath);
                    }

                    if (expanded != isExpanded(selPath)) {
                        //состояние узла поменялось - больше ничего не делаем
                        return;
                    }
                }
            }
            if (origAction != null) {
                origAction.actionPerformed(e);
            }
        }
    }
}
