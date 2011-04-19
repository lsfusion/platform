package platform.client.form.tree;

import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.cell.CellTableInterface;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.grid.GridTable;
import platform.client.form.sort.MultiLineHeaderRenderer;
import platform.client.form.sort.TableSortableHeaderManager;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientTreeGroup;
import platform.interop.Order;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class TreeGroupTable extends ClientFormTreeTable implements CellTableInterface {
    public static Color SELECTED_CELL_COLOR = new Color(192, 192, 255);
    public static Color FOCUSED_SELECTED_CELL_COLOR = new Color(128, 128, 255);

    private final TreeGroupNode rootNode;
    public final ClientFormController form;
    private final ClientTreeGroup treeGroup;

    public GroupTreeTableModel model;

    private boolean synchronize = false;

    private Action moveToNextCellAction = null;

    private ClientGroupObjectValue currentPath;
    public TreePath currentTreePath;
    private Set<TreePath> expandedPathes;

    boolean plainTreeMode = false;

    private TableSortableHeaderManager<ClientPropertyDraw> sortableHeaderManager;

    public TreeGroupTable(ClientFormController iform, ClientTreeGroup itreeGroup) {
        form = iform;
        treeGroup = itreeGroup;
        plainTreeMode = itreeGroup.plainTreeMode;

        setTreeTableModel(model = new GroupTreeTableModel(form, plainTreeMode));
        setupHierarhicalColumn();

        //после создания колонки для дерева, занимаемся созданием и удалением сами
        setAutoCreateColumnsFromModel(false);

        rootNode = model.getRoot();

        sortableHeaderManager = new TableSortableHeaderManager<ClientPropertyDraw>(this, true) {
            protected void orderChanged(ClientPropertyDraw columnKey, Order modiType) {
                TreeGroupTable.this.orderChanged(columnKey, modiType);
            }

            @Override
            protected ClientPropertyDraw getColumnKey(int column) {
                return model.getColumnProperty(column);
            }
        };

        tableHeader.setDefaultRenderer(new MultiLineHeaderRenderer(tableHeader.getDefaultRenderer(), sortableHeaderManager) {
            @Override
            public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                if (column > 0) {
                    model.getColumnProperty(column).design.designHeader(comp);
                }
                return comp;
            }
        });
        tableHeader.addMouseListener(sortableHeaderManager);

        if (plainTreeMode) {
            setTableHeader(null);
            setShowGrid(false, false);
        } else {
            //подсветка ячеек дерева
            setHighlighters(
                    new ColorHighlighter(
                            new HighlightPredicate.AndHighlightPredicate(
                                    HighlightPredicate.HAS_FOCUS,
                                    new HighlightPredicate.ColumnHighlightPredicate(0)
                            ), FOCUSED_SELECTED_CELL_COLOR, Color.BLACK, FOCUSED_SELECTED_CELL_COLOR, Color.BLACK
                    ),
                    new ColorHighlighter(
                            new HighlightPredicate.AndHighlightPredicate(
                                    new HighlightPredicate.NotHighlightPredicate(
                                            HighlightPredicate.HAS_FOCUS
                                    ),
                                    new HighlightPredicate.ColumnHighlightPredicate(0)
                            ), Color.WHITE, Color.BLACK, SELECTED_CELL_COLOR, Color.BLACK
                    )
            );

            setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
            setDefaultEditor(Object.class, new ClientAbstractCellEditor());
        }

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize) {
                    if (node.group != null) {
                        try {
                            form.expandGroupObject(node.group, node.key);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при открытии узла дерева.");
                        }
                    }
                }
                if (node.hasOnlyExpandningNodeAsChild()) {
                    throw new ExpandVetoException(event);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (synchronize || e.getValueIsAdjusting()) {
                    return;
                }

                final TreePath path = getPathForRow(getSelectedRow());
                if (path != null) {
                    TreeGroupNode node = (TreeGroupNode) path.getLastPathComponent();
                    if (node.group != null && currentPath != node.key) {
                        currentPath = node.key;
                        currentTreePath = path;

                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new ChangeObjectEvent(node.group, node.key));
                    }
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (!isEditing()) {
                    moveToFocusableCellIfNeeded();
                }
            }
        });

        addKeyListener(new TreeGroupQuickSearchHandler(this));

        initializeActionMap();
        currentTreePath = new TreePath(rootNode);
    }

    private void orderChanged(ClientPropertyDraw columnKey, Order modiType) {
        try {
            form.changeOrder(columnKey, modiType, new ClientGroupObjectValue());
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка изменении сортировки", e);
        }

        tableHeader.resizeAndRepaint();
    }

    private void initializeActionMap() {
        final Action nextColumnAction = new GotNextCellAction(0, 1);
        final Action prevColumnAction = new GotNextCellAction(0, -1);
        final Action nextRowAction = new GotNextCellAction(1, 0);
        final Action prevRowAction = new GotNextCellAction(-1, 0);

        final Action firstColumnAction = new GotNextCellAction(0, 1, true);
        final Action lastColumnAction = new GotNextCellAction(0, -1, true);

        final Action firstRowAction = new GotNextCellAction(1, 0, true);
        final Action lastRowAction = new GotNextCellAction(-1, 0, true);

        ActionMap actionMap = getActionMap();

        // вверх/вниз
        actionMap.put("selectNextRow", nextRowAction);
        actionMap.put("selectPreviousRow", prevRowAction);

        // вперёд/назад
        actionMap.put("selectNextColumn", new ExpandAction(true, nextColumnAction));
        actionMap.put("selectPreviousColumn", new ExpandAction(false, prevColumnAction));
        actionMap.put("selectNextColumnCell", nextColumnAction);
        actionMap.put("selectPreviousColumnCell", prevColumnAction);

        // в начало/конец ряда
        actionMap.put("selectFirstColumn", firstColumnAction);
        actionMap.put("selectLastColumn", lastColumnAction);

        // в начало/конец колонки
        actionMap.put("selectFirstRow", firstRowAction);
        actionMap.put("selectLastRow", lastRowAction);

        this.moveToNextCellAction = nextColumnAction;
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents) {
        model.updateKeys(group, keys, parents);
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        model.updateDrawPropertyValues(property, ivalues);
    }

    public boolean addDrawProperty(ClientGroupObject group, ClientPropertyDraw property) {
        int ind = model.addDrawProperty(form, group, property);
        if (ind > 0 -1 && !plainTreeMode) {
            createNewColumn(property, ind);
        }
        return ind != -1;
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        int ind = model.removeProperty(group, property);
        if (ind > 0 && !plainTreeMode) {
            removeColumn(getColumn(ind));
            //нужно поменять реальный индекс всех колонок после данной
            for (int i = ind; i < getColumnCount(); ++i) {
                getColumn(i).setModelIndex(i);
            }
        }
    }

    private void setupHierarhicalColumn() {
        TableColumnExt tableColumn = getColumnExt(0);
        int min = 50;
        int max = 100000;
        int pref = treeGroup.groups.size() * 35;

        setColumnSizes(tableColumn, min, max, pref);
    }

    private void createNewColumn(ClientPropertyDraw property, int pos) {
        TableColumnExt tableColumn = getColumnFactory().createAndConfigureTableColumn(getModel(), pos);
        if (tableColumn != null) {
            int min = property.getMinimumWidth(this);
            int max = property.getMaximumWidth(this);
            int pref = property.getPreferredWidth(this);

            setColumnSizes(tableColumn, min, max, pref);

            addColumn(tableColumn);
            moveColumn(getColumnCount() - 1, pos);
            //нужно поменять реальный индекс всех колонок после данной
            for (int i = pos + 1; i < getColumnCount(); ++i) {
                getColumn(i).setModelIndex(i);
            }

            String propCaption = !BaseUtils.isRedundantString(property.toolTip) ? property.toolTip : model.getColumnName(pos);
            String sid = property.getSID();
            String tableName = property.tableName != null ? property.tableName : "&lt;none&gt;";
            String ifaceObjects = BaseUtils.toString(property.interfacesCaptions, ", ");
            String ifaceClasses = BaseUtils.toString(property.interfacesTypes, ", ");
            String returnClass = property.returnClass.toString();

            tableColumn.setToolTipText(
                    String.format(GridTable.toolTipFormat, propCaption, sid, tableName, ifaceObjects, ifaceClasses, returnClass));
        }
    }

    private void setColumnSizes(TableColumnExt tableColumn, int min, int max, int pref) {
        // делаем так потому, что новые значения размеров
        // корректируется в зависимости от старых
        // и мы можем увидеть не то, что хотели
        if (min < tableColumn.getMinWidth()) {
            tableColumn.setMinWidth(min);
            tableColumn.setMaxWidth(max);
        } else {
            tableColumn.setMaxWidth(max);
            tableColumn.setMinWidth(min);
        }
        tableColumn.setPreferredWidth(pref);
    }

    public void setCurrentObjects(final ClientGroupObjectValue objects) {
        enumerateNodesDepthFirst(new NodeProccessor() {
            @Override
            public void processPath(TreePath nodePath) {
                Object node = nodePath.getLastPathComponent();
                if (node instanceof TreeGroupNode) {
                    TreeGroupNode groupNode = (TreeGroupNode) node;
                    if (groupNode.key.equals(objects)) {
                        currentPath = objects;
                        currentTreePath = nodePath;
                    }
                }
            }
        });
    }

    public void setSelectionPath(TreePath treePath) {
        getSelectionModel().setSelectionInterval(0, getRowForPath(treePath));
    }

    public void saveVisualState() {
        Enumeration paths = getExpandedDescendants(new TreePath(rootNode));
        expandedPathes = paths == null
                         ? new HashSet<TreePath>()
                         : new HashSet<TreePath>(Collections.list(paths));
    }

    public void restoreVisualState() {
        synchronize = true;
        enumerateNodesDepthFirst(new NodeProccessor() {
            @Override
            public void processPath(TreePath nodePath) {
                if (expandedPathes.contains(nodePath)) {
                    expandPath(nodePath);
                }
                model.firePathChanged(nodePath);
            }
        });

        moveToFocusableCellIfNeeded();

        setSelectionPath(currentTreePath);

        synchronize = false;
    }

    private boolean isCellFocusable(int row, int col) {
        TreePath path = getPathForRow(row);
        return path != null && model.isCellFocusable(path.getLastPathComponent(), col);
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        if (toggle) {
            //чтобы нельзя было просто убрать выделение
            return;
        }

        if (isCellFocusable(row, col)) {
            super.changeSelection(row, col, toggle, extend);
        }
    }

    @Override
    public boolean isDataChanging() {
        return true;
    }

    @Override
    public ClientPropertyDraw getProperty(int row, int column) {
        TreePath pathForRow = getPathForRow(row);
        if (pathForRow != null) {
            return model.getProperty(pathForRow.getLastPathComponent(), column);
        }
        return null;
    }

    @Override
    public boolean isCellHighlighted(int row, int column) {
        //todo:
        return false;
    }

    @Override
    public Color getHighlightColor(int row, int column) {
        //todo:
        return null;
    }

    @Override
    public ClientFormController getForm() {
        return form;
    }

    public Object convertValueFromString(String value, int row, int column) {
        if (column == 0) {
            return null;
        }

        ClientPropertyDraw property = getProperty(row, column);
        if (property != null) {
            try {
                return property.parseString(form, value);
            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    public ClientPropertyDraw getCurrentProperty() {
        int column = getSelectedColumn();
        int row = getSelectedRow();

        ClientPropertyDraw selectedProperty = null;

        if (column == 0) {
            ++column;
        }

        if (column >= 0 && column < getColumnCount() && row >= 0 && row <= getRowCount()) {
            selectedProperty = getProperty(row, column);
        }

        return selectedProperty != null
               ? selectedProperty
               : model.getColumnCount() > 1
                 ? model.getColumnProperty(1)
                 : null;
    }

    public Object getSelectedValue(ClientPropertyDraw property) {
        int row = getSelectedRow();

        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getPropertyValue(pathForRow.getLastPathComponent(), property);
    }

    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            int row = editingRow;
            int column = editingColumn;
            //переопределяем, чтобы сначала удалить editor, и только потом выставлять значение
            //иначе во время выставления таблица всё ещё находится в режиме редактирования
            removeEditor();
            setValueAt(value, row, column);
        }
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        int propertyIndex = model.getPropertyColumnIndex(property);
        if (propertyIndex > 0) {
            sortableHeaderManager.changeOrder(property, modiType);
        } else {
            //меняем напрямую для верхних groupObjects
            form.changeOrder(property, modiType, new ClientGroupObjectValue());
        }
    }

    private class ChangeObjectEvent extends AWTEvent implements ActiveEvent {
        public static final int CHANGE_OBJECT_EVENT = AWTEvent.RESERVED_ID_MAX + 5555;
        private final ClientGroupObject group;
        private final ClientGroupObjectValue key;

        public ChangeObjectEvent(ClientGroupObject group, ClientGroupObjectValue key) {
            super(TreeGroupTable.this, CHANGE_OBJECT_EVENT);
            this.group = group;
            this.key = key;
        }

        @Override
        public void dispatch() {
            if (key == currentPath) {
                try {
                    form.changeGroupObject(group, key);
                } catch (IOException ioe) {
                    throw new RuntimeException("Ошибка при выборе узла.", ioe);
                }
            }
        }
    }

    private class GotNextCellAction extends AbstractAction {
        private final int dc;
        private final int dr;
        private final boolean boundary;

        public GotNextCellAction(int dr, int dc) {
            this(dr, dc, false);
        }

        public GotNextCellAction(int dr, int dc, boolean boundary) {
            this.dr = dr;
            this.dc = dc;
            this.boundary = boundary;
        }

        private int[] moveNext(int row, int column, int maxR, int maxC) {
            if (dc != 0) {
                column += dc;
                if (column > maxC) {
                    column = 0;
                    if (!boundary) {
                        ++row;
                        if (row > maxR) {
                            row = 0;
                        }
                    }
                } else if (column < 0) {
                    column = maxC;
                    if (!boundary) {
                        --row;
                        if (row < 0) {
                            row = maxR;
                        }
                    }
                }
            } else {
                row += dr;
                if (row > maxR) {
                    row = 0;
                } else if (row < 0) {
                    row = maxR;
                }
            }
            return new int[]{row, column};
        }

        public void actionPerformed(ActionEvent e) {
            if (isEditing() && !getCellEditor().stopCellEditing()) {
                return;
            }
            if (getRowCount() <= 0 || getColumnCount() <= 0) {
                return;
            }

            int maxR = getRowCount() - 1;
            int maxC = getColumnCount() - 1;

            int startRow = boundary && dr != 0
                           ? (dr > 0 ? 0 : maxR)
                           : getSelectionModel().getLeadSelectionIndex();

            int startColumn = boundary && dc != 0
                              ? (dc > 0 ? 0 : maxC)
                              : getColumnModel().getSelectionModel().getLeadSelectionIndex();

            boolean checkFirst = startRow == -1 || startColumn == -1 || boundary;

            int row = startRow = startRow == -1 ? 0 : startRow;
            int column = startColumn = startColumn == -1 ? 0 : startColumn;

            if (!(checkFirst && isCellFocusable(row, column))) {
                do {
                    int next[] = moveNext(row, column, maxR, maxC);
                    row = next[0];
                    column = next[1];
                } while ((row != startRow || column != startColumn) && !isCellFocusable(row, column));
            }

            changeSelection(row, column, false, false);
        }
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JXTableHeader(columnModel) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                return new Dimension(pref.width, 34);
            }
        };
    }
}
