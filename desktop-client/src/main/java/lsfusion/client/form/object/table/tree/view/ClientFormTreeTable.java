package lsfusion.client.form.object.table.tree.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.table.view.TableTransferHandler;
import lsfusion.interop.form.event.KeyStrokes;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;

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
        //remove default enter and shift-enter actions
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getShiftEnter(), "none");
    }

    @Override
    public void updateUI() {
        // gridColor is never updated in updateUI() if it was once set.
        // not using setter in order not to call repaint() once more - it will be called in updateUI() after color theme change
        gridColor = SwingDefaults.getTableGridColor();
        
        super.updateUI();
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
        void processPath(TreePath nodePath);
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
            Enumeration<MutableTreeTableNode> en = (Enumeration<MutableTreeTableNode>) parentNode.children();
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
