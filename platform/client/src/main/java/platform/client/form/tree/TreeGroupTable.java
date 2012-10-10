package platform.client.form.tree;

import com.google.common.base.Preconditions;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableNode;
import platform.client.ClientResourceBundle;
import platform.client.form.*;
import platform.client.form.cell.CellTableInterface;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.dispatch.EditPropertyDispatcher;
import platform.client.form.dispatch.SimpleChangePropertyDispatcher;
import platform.client.form.sort.MultiLineHeaderRenderer;
import platform.client.form.sort.TableSortableHeaderManager;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientTreeGroup;
import platform.client.logics.classes.ClientType;
import platform.interop.Order;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class TreeGroupTable extends ClientFormTreeTable implements CellTableInterface, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher = new EditPropertyDispatcher(this);
    private final SimpleChangePropertyDispatcher pasteDispatcher;

    private final EditBindingMap editBindingMap = new EditBindingMap();

    private final CellTableContextMenuHandler contextMenuHandler = new CellTableContextMenuHandler(this);

    protected EventObject editEvent;
    protected int editRow;
    protected int editCol;
    protected ClientType currentEditType;
    protected Object currentEditValue;
    protected boolean editPerformed;
    protected boolean commitingValue;

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
    boolean manualExpand;

    private TableSortableHeaderManager<ClientPropertyDraw> sortableHeaderManager;

    public TreeGroupTable(ClientFormController iform, ClientTreeGroup itreeGroup) {
        form = iform;
        treeGroup = itreeGroup;
        plainTreeMode = itreeGroup.plainTreeMode;
        pasteDispatcher = form.getSimpleChangePropertyDispatcher();

        contextMenuHandler.install();

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
            protected void ordersCleared(ClientGroupObject groupObject) {
                TreeGroupTable.this.ordersCleared(groupObject);
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
                            ), PropertyRendererComponent.FOCUSED_CELL_BACKGROUND, Color.BLACK, PropertyRendererComponent.FOCUSED_CELL_BACKGROUND, Color.BLACK
                    ),
                    new ColorHighlighter(
                            new HighlightPredicate.AndHighlightPredicate(
                                    new HighlightPredicate.NotHighlightPredicate(
                                            HighlightPredicate.HAS_FOCUS
                                    ),
                                    new HighlightPredicate.ColumnHighlightPredicate(0)
                            ), Color.WHITE, Color.BLACK, PropertyRendererComponent.SELECTED_ROW_BACKGROUND, Color.BLACK
                    )
            );

            setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
            setDefaultEditor(Object.class, new ClientAbstractCellEditor(this));
        }

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        try {
                            form.expandGroupObject(node.group, node.key);
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"), e);
                        }
                    }
                }
                if (node.hasOnlyExpandningNodeAsChild()) {
                    throw new ExpandVetoException(event);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        try {
                            form.collapseGroupObject(node.group, node.key);
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"), e);
                        }
                    }
                }
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (synchronize) {
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

        if (form.isDialog()) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        form.okPressed();
                    }
                }
            });
        }

        initializeActionMap();
        currentTreePath = new TreePath(rootNode);
    }

    private void orderChanged(ClientPropertyDraw columnKey, Order modiType) {
        try {
            form.changePropertyOrder(columnKey, modiType, ClientGroupObjectValue.EMPTY);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.sorting"), e);
        }

        tableHeader.resizeAndRepaint();
    }

    private void ordersCleared(ClientGroupObject groupObject) {
        try {
            form.clearPropertyOrders(groupObject);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.sorting"), e);
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

    public void expandNew(ClientGroupObject group) {
        for (TreeGroupNode node : model.getGroupNodes(group)) {
            boolean inExpanded = false;
            for (TreePath expandedPath : expandedPathes)
                if (Arrays.asList(expandedPath.getPath()).contains(node))
                    inExpanded = true;
            if (!inExpanded && (node.getChildCount() > 1 || (node.getChildCount() == 1 && node.getChildAt(0) instanceof TreeGroupNode))) {
                TreePath path = new TreePath(getTreePath(node, new ArrayList<TreeTableNode>()).toArray());
                expandPathManually(path);
            }
        }
    }
    
    private List<TreeTableNode> getTreePath(TreeTableNode node, List<TreeTableNode> path) {
        path.add(0, node);
        if (node.getParent() != null) {
            return getTreePath(node.getParent(), path);
        } else {
            return path;
        }
    }
    
    public void expandPathManually(TreePath path) {
        manualExpand = true;
        expandPath(path);
        manualExpand = false;
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues, boolean update) {
        model.updateDrawPropertyValues(property, ivalues, update);
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
        int pref = treeGroup.calculatePreferredSize();

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

            tableColumn.setToolTipText(property.getTooltipText(model.getColumnName(pos)));
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

    public void setCurrentPath(final ClientGroupObjectValue objects) {
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

        for (ClientGroupObject group : treeGroup.groups)
            expandNew(group);

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

    public boolean isPressed(int row, int column) {
        return false;
    }

    @Override
    public ClientPropertyDraw getProperty(int row, int column) {
        TreePath pathForRow = getPathForRow(row);
        if (pathForRow != null) {
            return model.getProperty(pathForRow.getLastPathComponent(), column);
        }
        return null;
    }

    public ClientGroupObjectValue getColumnKey(int row, int col) {
        TreePath pathForRow = getPathForRow(row);
        if (pathForRow != null) {
            Object node = pathForRow.getLastPathComponent();
            if (node instanceof TreeGroupNode)
                return ((TreeGroupNode) node).key;
        }
        return ClientGroupObjectValue.EMPTY;
    }

    public void pasteTable(List<List<String>> table) {
        //пока вставляем только одно значение

        int row = getSelectionModel().getLeadSelectionIndex();
        int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        if (!isHierarchical(column) && !table.isEmpty() && !table.get(0).isEmpty()) {
            ClientPropertyDraw property = getProperty(row, column);
            ClientGroupObjectValue columnKey = getColumnKey(row, column);
            if (property != null) {
                try {
                    Object parsedValue = property.parseString(form, columnKey, table.get(0).get(0), false);
                    pasteDispatcher.changeProperty(parsedValue, property, columnKey, true);
                } catch (ParseException ignored) {
                }
            }
        }
    }

    @Override
    public boolean isSelected(int row, int coulumn) {
        //todo:
        return false;
    }

    @Override
    public Color getBackgroundColor(int row, int column) {
        //todo:
        return null;
    }

    @Override
    public Color getForegroundColor(int row, int column) {
        return null;
    }

    @Override
    public ClientFormController getForm() {
        return form;
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

    @Override
    public ClientType getCurrentEditType() {
        return currentEditType;
    }

    @Override
    public Object getCurrentEditValue() {
        return currentEditValue;
    }

    private String getEditActionSID(EventObject e, ClientPropertyDraw property) {
        String actionSID = null;
        if (property.editBindingMap != null) {
            actionSID = property.editBindingMap.getAction(e);
        }

        if (actionSID == null) {
            actionSID = editBindingMap.getAction(e);
        }
        return actionSID;
    }

    public boolean editCellAt(int row, int column, EventObject e){
        if (!form.commitCurrentEditing()) {
            return false;
        }

        if (e instanceof MouseEvent) {
            // чтобы не срабатывало редактирование при изменении ряда,
            // потому что всё равно будет апдейт
            int selRow = getSelectedRow();
            if (selRow == -1 || selRow != row) {
                return false;
            }
        }

        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }

        if (!isCellEditable(row, column)) {
            return false;
        }

        if (isHierarchical(column)) {
            return false;
        }

        ClientPropertyDraw property = getProperty(row, column);
        if (property == null) {
            return false;
        }

        ClientGroupObjectValue columnKey = getColumnKey(row, column);
        if (columnKey == null) {
            return false;
        }

        String actionSID = getEditActionSID(e, property);
        if (actionSID != null) {
            editRow = row;
            editCol = column;
            editEvent = e;
            commitingValue = false;

            //здесь немного запутанная схема...
            //executePropertyEditAction возвращает true, если редактирование произошло на сервере, необязательно с вводом значения...
            //но из этого editCellAt мы должны вернуть true, только если началось редактирование значения
            editPerformed = editDispatcher.executePropertyEditAction(property, columnKey, actionSID, getValueAt(row, column));
            return editorComp != null;
        }

        return false;
    }

    public boolean requestValue(ClientType valueType, Object oldValue) {
        //пока чтение значения можно вызывать только один раз в одном изменении...
        //если получится безусловно задержать фокус, то это ограничение можно будет убрать
        Preconditions.checkState(!commitingValue, "You can request value only once per edit action.");

        currentEditType = valueType;
        currentEditValue = oldValue;

        if (!super.editCellAt(editRow, editCol, editEvent)) {
            return false;
        }

        if (editEvent instanceof KeyEvent) {
            prepareTextEditor();
        }

        editorComp.requestFocusInWindow();

        form.setCurrentEditingTable(this);

        return true;
    }

    void prepareTextEditor() {
        if (editorComp instanceof JTextComponent) {
            JTextComponent textEditor = (JTextComponent) editorComp;
            textEditor.selectAll();
            if (getProperty(editRow, editCol).clearText) {
                textEditor.setText("");
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component component = super.prepareEditor(editor, row, column);
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent)component;
            // у нас есть возможность редактировать нефокусную таблицу, и тогда после редактирования фокус теряется,
            // поэтому даём возможность FocusManager'у самому поставить фокус
            if (!isFocusable() && jComponent.getNextFocusableComponent() == this) {
                jComponent.setNextFocusableComponent(null);
                return component;
            }
        }
        return component;
    }

    private void commitValue(Object value) {
        commitingValue = true;
        editDispatcher.commitValue(value);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            internalRemoveEditor();
            commitValue(value);
        }
    }

    public void updateEditValue(Object value) {
        setValueAt(value, editRow, editCol);
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        internalRemoveEditor();
        editDispatcher.cancelEdit();
    }

    private void internalRemoveEditor() {
        super.removeEditor();
    }

    @Override
    public void removeEditor() {
        // removeEditor иногда вызывается напрямую, поэтому вызываем cancelCellEditing сами
        TableCellEditor cellEditor = getCellEditor();
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        editPerformed = false;
        boolean consumed = super.processKeyBinding(ks, e, condition, pressed);
        return consumed || editPerformed;
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        int propertyIndex = model.getPropertyColumnIndex(property);
        if (propertyIndex > 0) {
            sortableHeaderManager.changeOrder(property, modiType);
        } else {
            //меняем напрямую для верхних groupObjects
            form.changePropertyOrder(property, modiType, ClientGroupObjectValue.EMPTY);
        }
    }

    public void clearOrders(ClientGroupObject groupObject) throws IOException {
            sortableHeaderManager.clearOrders(groupObject);
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
                    throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.selecting.node"), ioe);
                }
            }
        }
    }

    public ClientGroupObjectValue getCurrentPath() {
        return currentPath;
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
            if (!form.commitCurrentEditing()) {
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
}
