package platform.client.form.tree;

import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.EventObject;

public abstract class ClientFormTreeTable extends JXTreeTable {

    protected ClientFormTreeTable() {

        setToggleClickCount(-1);

        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setSurrendersFocusOnKeystroke(true);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);

        setShowGrid(true, true);

        setupActionMap();

        setTransferHandler(new TreeTableTransferHandler(this));
    }

    private void setupActionMap() {
        //  Have the enter key work the same as the tab key
        InputMap im = getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStrokes.getEnter(), im.get(KeyStrokes.getTab()));

        am.put("selectNextColumn", new ExpandAction(true, am.get("selectNextColumn")));
        am.put("selectPreviousColumn", new ExpandAction(false, am.get("selectPreviousColumn")));
    }

    public boolean editCellAt(int row, int column, EventObject e){
        if (e instanceof MouseEvent) {
            // чтобы не срабатывало редактирование при изменении ряда,
            // потому что всё равно будет апдейт
            int selRow = getSelectedRow();
            if (selRow == -1 || selRow != row) {
                return false;
            }
        }

        boolean result = super.editCellAt(row, column, e);
        if (result) {
            final Component editor = getEditorComponent();
            if (editor instanceof JTextComponent) {
                ((JTextComponent) editor).selectAll();
            }
        }

        return result;
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

    public abstract Object convertValueFromString(String value, int row, int column);

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
