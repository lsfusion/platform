package lsfusion.client.form.object.table.tree.view;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.data.ClientLogicalClass;
import lsfusion.client.classes.data.ClientTextClass;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.object.table.tree.GroupTreeTableModel;
import lsfusion.client.form.object.table.tree.TreeGroupNode;
import lsfusion.client.form.object.table.view.GridPropertyTable;
import lsfusion.client.form.order.user.MultiLineHeaderRenderer;
import lsfusion.client.form.order.user.TableSortableHeaderManager;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.controller.ClientAbstractCellEditor;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.cell.view.ClientAbstractCellRenderer;
import lsfusion.client.form.property.table.view.CellTableContextMenuHandler;
import lsfusion.client.form.property.table.view.CellTableInterface;
import lsfusion.client.form.property.table.view.InternalEditEvent;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.order.user.Order;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableNode;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.*;

import static java.lang.Math.max;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static lsfusion.client.base.view.SwingDefaults.getTableHeaderHeight;
import static lsfusion.client.form.controller.ClientFormController.PasteData;
import static lsfusion.client.form.property.cell.EditBindingMap.getPropertyEventActionSID;
import static lsfusion.client.form.property.cell.EditBindingMap.isEditableAwareEditEvent;

public class TreeGroupTable extends ClientFormTreeTable implements CellTableInterface {
    private final EditPropertyDispatcher editDispatcher;

    private final EditBindingMap editBindingMap = new EditBindingMap();

    private final CellTableContextMenuHandler contextMenuHandler = new CellTableContextMenuHandler(this);

    private final int HIERARCHICAL_COLUMN_MIN_WIDTH = 50;
    private final int HIERARCHICAL_COLUMN_MAX_WIDTH = 100000;

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

    private WeakReference<TableCellRenderer> defaultHeaderRendererRef;
    private TableCellRenderer wrapperHeaderRenderer;

    public TreeGroupTable(ClientFormController iform, ClientTreeGroup itreeGroup) {
        form = iform;
        treeGroup = itreeGroup;
        plainTreeMode = itreeGroup.plainTreeMode;

        editDispatcher = new EditPropertyDispatcher(this, form.getDispatcherListener());

        contextMenuHandler.install();
        setAutoCreateColumnsFromModel(false);

        setTreeTableModel(model = new GroupTreeTableModel(form, plainTreeMode));
        
        addColumn(createColumn(0)); // одна колонка для дерева. создаём вручную, чтобы подставить renderer
        
        setupHierarhicalColumn();

        rootNode = model.getRoot();
        
        if (treeGroup.design.font != null) {
            setFont(treeGroup.design.getFont(this));
        }

        sortableHeaderManager = new TableSortableHeaderManager<ClientPropertyDraw>(this, true) {
            protected void orderChanged(final ClientPropertyDraw columnKey, final Order modiType, boolean alreadySet) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        TreeGroupTable.this.orderChanged(columnKey, modiType, alreadySet);
                    }
                });
            }

            @Override
            protected void ordersCleared(final ClientGroupObject groupObject) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        TreeGroupTable.this.ordersCleared(groupObject);
                    }
                });
            }

            @Override
            protected ClientPropertyDraw getColumnKey(int column) {
                return model.getColumnProperty(column);
            }

            @Override
            protected ClientPropertyDraw getColumnProperty(int column) {
                return model.getColumnProperty(column);
            }
        };

        tableHeader.addMouseListener(sortableHeaderManager);

        if (plainTreeMode) {
            setTableHeader(null);
            setShowGrid(false, false);
        } else {
            setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
            setDefaultEditor(Object.class, new ClientAbstractCellEditor(this));
        }

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                final TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                form.expandGroupObject(node.group, node.key);
                            }
                        });
                    }
                }
                if (node.hasOnlyExpandningNodeAsChild()) {
                    throw new ExpandVetoException(event);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        form.collapseGroupObject(node.group, node.key);
                    }
                }
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (synchronize || model.synchronize) {
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
        
        if (treeGroup.expandOnClick) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && !editPerformed) {
                        final TreePath path = getPathForRow(rowAtPoint(e.getPoint()));
                        if (path != null && !isLocationInExpandControl(getHierarhicalColumnRenderer().getUI(), path, e.getX(), e.getY())) {
                            final TreeGroupNode node = (TreeGroupNode) path.getLastPathComponent();

                            if (node.isExpandable() && node.group != null) {
                                RmiQueue.runAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isExpanded(path)) {
                                            form.expandGroupObject(node.group, node.key);
                                        } else {
                                            form.collapseGroupObject(node.group, node.key);
                                        }
                                        e.consume();
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!e.isConsumed()) {
                    ClientPropertyDraw property = getSelectedProperty();
                    //игнорируем double click по editable boolean
                    boolean ignore = property != null && property.baseType instanceof ClientLogicalClass && !property.isReadOnly();
                    if (!ignore)
                        form.processBinding(new MouseInputEvent(e), null, () -> null);
                }
            }
        });

        initializeActionMap();
        currentTreePath = new TreePath(rootNode);
    }

    private void orderChanged(ClientPropertyDraw columnKey, Order modiType, boolean alreadySet) {
        form.changePropertyOrder(columnKey, modiType, ClientGroupObjectValue.EMPTY, alreadySet);
        tableHeader.resizeAndRepaint();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return gridPropertyTable.getScrollableTracksViewportWidth();
    }

    @Override
    public void doLayout() {
        gridPropertyTable.doLayout();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return treeGroup.autoSize ? getPreferredSize() : SwingDefaults.getTablePreferredSize();
    }

    private void ordersCleared(ClientGroupObject groupObject) {
        form.clearPropertyOrders(groupObject);
        tableHeader.resizeAndRepaint();
    }

    private void initializeActionMap() {

        if(treeGroup != null) {
            form.addBinding(new KeyInputEvent(KeyStrokes.getEnter()), getEnterBinding(false));
            form.addBinding(new KeyInputEvent(KeyStrokes.getShiftEnter()), getEnterBinding(true));
        }

        final Action nextColumnAction = new GoToNextCellAction(0, 1);
        final Action prevColumnAction = new GoToNextCellAction(0, -1);
        final Action nextRowAction = new GoToNextCellAction(1, 0);
        final Action prevRowAction = new GoToNextCellAction(-1, 0);

        final Action firstColumnAction = new GoToNextCellAction(0, -1, true);
        final Action lastColumnAction = new GoToNextCellAction(0, 1, true);

        final Action firstRowAction = new GoToNextCellAction(-1, 0, true);
        final Action lastRowAction = new GoToNextCellAction(1, 0, true);

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

    private ClientFormController.Binding getEnterBinding(boolean shiftPressed) {
        ClientFormController.Binding binding = new ClientFormController.Binding(BaseUtils.last(treeGroup.groups), -100) {
            @Override
            public boolean pressed(KeyEvent ke) {
                tabAction(!shiftPressed);
                return true;
            }
            @Override
            public boolean showing() {
                return true;
            }
        };
        binding.bindEditing = BindingMode.NO;
        binding.bindGroup = BindingMode.ONLY;
        return binding;
    }

    private Action tabAction = new GoToNextCellAction(0, 1);
    private Action shiftTabAction = new GoToNextCellAction(0, -1);

    protected void tabAction(boolean forward) {
        (forward ? tabAction : shiftTabAction).actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents, Map<ClientGroupObjectValue, Boolean> expandables) {
        model.updateKeys(group, keys, parents, expandables);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        model.updateReadOnlyValues(property, readOnlyValues);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        model.updateCellBackgroundValues(property, cellBackgroundValues);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        model.updateCellForegroundValues(property, cellForegroundValues);
    }

    public void updateImageValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> imageValues) {
        model.updateImageValues(property, imageValues);
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        model.updateRowBackgroundValues(rowBackground);
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        model.updateRowForegroundValues(rowForeground);
    }

    public void expandNew(ClientGroupObject group) {
        for (TreeGroupNode node : model.getGroupNodes(group)) {
            boolean inExpanded = false;
            for (TreePath expandedPath : expandedPathes)
                if (Arrays.asList(expandedPath.getPath()).contains(node))
                    inExpanded = true;
            if (!inExpanded && (node.getChildCount() > 1 || (node.getChildCount() == 1 && node.getChildAt(0) instanceof TreeGroupNode))) {
                TreePath path = new TreePath(getTreePath(node, new ArrayList<>()).toArray());
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

    private Map<ClientPropertyDraw, TableColumn> columnsMap = MapFact.mAddRemoveMap();

    public boolean addDrawProperty(ClientGroupObject group, ClientPropertyDraw property) {
        int ind = model.addDrawProperty(form, group, property);
        if (ind > -1 && !plainTreeMode) {
            createPropertyColumn(property, ind);
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

            columnsMap.remove(property);
        }
        
        int rowHeight = 0;
        for (ClientPropertyDraw columnProperty : model.columnProperties) {
            rowHeight = max(rowHeight, columnProperty.getValueHeight(this));
        }
        if (rowHeight != getRowHeight() && rowHeight > 0) {
            setTableRowHeight(rowHeight);
        }
    }

    private int hierarchicalWidth;
    private TableColumnExt hierarchicalColumn;
    private void setupHierarhicalColumn() {
        TableColumnExt tableColumn = getColumnExt(0);

        hierarchicalColumn = tableColumn;
        hierarchicalWidth = treeGroup.calculateSize();
//        setColumnSizes(tableColumn, pref, pref, pref);

        getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
    }

    private TableColumnExt createColumn(int pos) {
        TableColumnExt tableColumn = new TableColumnExt(pos) {
            @Override
            public TableCellRenderer getHeaderRenderer() {
                TableCellRenderer defaultHeaderRenderer = tableHeader.getDefaultRenderer();
                if (defaultHeaderRendererRef == null || defaultHeaderRendererRef.get() != defaultHeaderRenderer) {
                    defaultHeaderRendererRef = new WeakReference<>(defaultHeaderRenderer);
                    wrapperHeaderRenderer = new MultiLineHeaderRenderer(tableHeader.getDefaultRenderer(), sortableHeaderManager) {
                        @Override
                        public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                            if (column > 0) {
                                model.getColumnProperty(column).design.designHeader(comp);
                            }
                            return comp;
                        }
                    };
                }
                return wrapperHeaderRenderer;
            }
        };
        getColumnFactory().configureTableColumn(getModel(), tableColumn);
        return tableColumn;
    } 

    private void createPropertyColumn(ClientPropertyDraw property, int pos) {
        TableColumnExt tableColumn = createColumn(pos);
        int rowHeight = getRowHeight();
        int currentSelectedColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();

//        int min = property.getMinimumValueWidth(this);
//        int max = property.getMaximumValueWidth(this);
//        int pref = property.getPreferredValueWidth(this);
//
//        setColumnSizes(tableColumn, min, max, pref);

        rowHeight = max(rowHeight, property.getValueHeight(this));

        addColumn(tableColumn);
        moveColumn(getColumnCount() - 1, pos);
        //нужно поменять реальный индекс всех колонок после данной
        for (int i = pos + 1; i < getColumnCount(); ++i) {
            getColumn(i).setModelIndex(i);
        }

        // moveColumn норовит выделить вновь добавленную колонку (при инициализации происходит скроллирование вправо). возвращаем выделение обратно
        if (currentSelectedColumn != -1) {
            getColumnModel().getSelectionModel().setLeadSelectionIndex(pos <= currentSelectedColumn ? currentSelectedColumn + 1 : currentSelectedColumn);
        }

        tableColumn.setToolTipText(property.getTooltipText(model.getColumnName(pos)));

        columnsMap.put(property, tableColumn);

        if (getRowHeight() != rowHeight && rowHeight > 0) {
            setTableRowHeight(rowHeight);
        }
    }
    
    private void setTableRowHeight(int rowHeight) {
        // cell height/width is calculated without row/column margins (getCellRect()). Row/column margin = intercell spacing.
        int newRowHeight = rowHeight + getRowMargin();
        if (getRowHeight() != newRowHeight && newRowHeight > 0) {
            setRowHeight(newRowHeight);
        }
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
                         ? new HashSet<>()
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
//        TreePath pathForRow = getPathForRow(row);
//        if (pathForRow != null) {
//            Object node = pathForRow.getLastPathComponent();
//            if (node instanceof TreeGroupNode)
//                return ((TreeGroupNode) node).key;
//        }
        return ClientGroupObjectValue.EMPTY;
    }

    @Override
    public boolean richTextSelected() {
        ClientPropertyDraw property = getSelectedProperty();
        return property != null && property.baseType instanceof ClientTextClass && ((ClientTextClass) property.baseType).rich;
    }

    public void pasteTable(List<List<String>> table) {
        //пока вставляем только одно значение

        int row = getSelectionModel().getLeadSelectionIndex();
        int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        if (!isHierarchical(column) && !table.isEmpty() && !table.get(0).isEmpty()) {

            final ClientPropertyDraw property = getProperty(row, column);
            if (property != null) {
                try {
                    String value = table.get(0).get(0);
                    Object newValue = value == null ? null : property.parseChangeValueOrNull(value);
                    if (property.canUsePasteValueForRendering()) {
                        model.setValueAt(newValue, row, column);
                    }

                    form.pasteMulticellValue(
                            singletonMap(property, new PasteData(newValue, singletonList(currentPath), singletonList(getValueAt(row, column))))
                    );
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    @Override
    public boolean paintSelected(int row, int column) {
        return false;
    }

    @Override
    public boolean hasSingleSelection() {
        return false;
    }

    @Override
    public Color getBackgroundColor(int row, int column) {
        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getBackgroundColor(pathForRow.getLastPathComponent(), column);
    }

    @Override
    public Color getForegroundColor(int row, int column) {
        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getForegroundColor(pathForRow.getLastPathComponent(), column);
    }

    @Override
    public ClientFormController getForm() {
        return form;
    }

    public ClientPropertyDraw getSelectedProperty() {
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
    
    @Override
    public Object getEditValue() {
        return getValueAt(editRow, editCol);
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

        ClientPropertyDraw property = getProperty(row, column);
        if (property == null) {
            return false;
        }

        ClientGroupObjectValue columnKey = getColumnKey(row, column);
        if (columnKey == null) {
            return false;
        }

        String actionSID = getPropertyEventActionSID(e, property, editBindingMap);
        if (actionSID == null) {
            return false;
        }

        if (isEditableAwareEditEvent(actionSID) && !isCellEditable(row, column)) {
            return false;
        }

        if (isHierarchical(column)) {
            return false;
        }

        //здесь немного запутанная схема...
        //executePropertyEventAction возвращает true, если редактирование произошло на сервере, необязательно с вводом значения...
        //но из этого editCellAt мы должны вернуть true, только если началось редактирование значения
        editPerformed = edit(actionSID, property, columnKey, row, column, e);
        return editorComp != null;
    }

    public boolean edit(String actionSID, ClientPropertyDraw property, ClientGroupObjectValue columnKey, int row, int column, EventObject e) {
        editRow = row;
        editCol = column;
        commitingValue = false;
        editEvent = e;

        return editDispatcher.executePropertyEventAction(property, columnKey, actionSID, editEvent);
    }

    public boolean requestValue(ClientType valueType, Object oldValue) {
        //пока чтение значения можно вызывать только один раз в одном изменении...
        //если получится безусловно задержать фокус, то это ограничение можно будет убрать
        Preconditions.checkState(!commitingValue, "You can request value only once per edit action.");

        // need this because we use getTableCellEditorComponent infrastructure and we need to pass currentEditValue there somehow
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

    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            internalRemoveEditor();
            commitValue(value);
        }
    }

    private void commitValue(Object value) {
        commitingValue = true;
        editDispatcher.commitValue(value);
        form.clearCurrentEditingTable(this);
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        internalRemoveEditor();
        editDispatcher.cancelEdit();
        form.clearCurrentEditingTable(this);
    }

    public void updateEditValue(Object value) {
        setValueAt(value, editRow, editCol);
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

    @Override
    protected void processKeyEvent(KeyEvent e) {
        int row = getSelectedRow();
        int column = getSelectedColumn();
        if (row >= 0 && row < getRowCount() && column >= 1 && column < getColumnCount()) {
            ClientPropertyDraw property = getProperty(row, column);
            ClientGroupObjectValue columnKey = getColumnKey(row, column);

            if (property != null && columnKey != null) {
                String keyPressedActionSID = EditBindingMap.getPropertyKeyPressActionSID(e, property);
                if (keyPressedActionSID != null) {
                    edit(keyPressedActionSID, property, columnKey, row, column, new InternalEditEvent(this, keyPressedActionSID));
                }
            }
        }
        
        super.processKeyEvent(e);
    }

    public boolean changeOrders(ClientGroupObject groupObject, LinkedHashMap<ClientPropertyDraw, Boolean> orders, boolean alreadySet) {
        return sortableHeaderManager.changeOrders(groupObject, orders, alreadySet);
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        JXTableHeader jxTableHeader = new JXTableHeader(columnModel) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                return new Dimension(pref.width, getTableHeaderHeight());
            }
        };
        jxTableHeader.setReorderingAllowed(false);
        return jxTableHeader;
    }

    public void updateTable() {
        gridPropertyTable.updateLayoutWidth();
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

    private class GoToNextCellAction extends AbstractAction {
        private final int dc;
        private final int dr;
        private final boolean boundary;

        public GoToNextCellAction(int dr, int dc) {
            this(dr, dc, false);
        }

        public GoToNextCellAction(int dr, int dc, boolean boundary) {
            this.dr = dr;
            this.dc = dc;
            this.boundary = boundary;
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

            int row = getSelectionModel().getLeadSelectionIndex();
            int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

            boolean checkFirst = row == -1 || column == -1;
            row = row == -1 ? 0 : row;
            column = column == -1 ? 0 : column;

            if (boundary) {
                int startRow = dr != 0
                               ? (dr > 0 ? 0 : maxR)
                               : row;

                int startColumn = dc != 0
                                  ? (dc > 0 ? 0 : maxC)
                                  : column;

                int endRow = dr != 0
                        ? (dr > 0 ? maxR : 0)
                        : startRow;
                int endColumn = dc != 0
                        ? (dc > 0 ? maxC : 0)
                        : startColumn;

                int rc[] = moveBoundary(startRow, endRow, startColumn, endColumn);
                row = rc[0];
                column = rc[1];
            } else {
                if (!(checkFirst && isCellFocusable(row, column))) {
                    do {
                        int next[] = moveNext(row, column, maxR, maxC);
                        row = next[0];
                        column = next[1];
                    } while (row != -1 && !isCellFocusable(row, column));
                }
            }

            if (row >= 0 && column >= 0) {
                changeSelection(row, column, false, false);
            }
        }

        private int[] moveNext(int row, int column, int maxR, int maxC) {
            if (dc != 0) {
                column += dc;
                if (column > maxC) {
                    ++row;
                    if (row > maxR) {
                        // в крайнем положении - больше некуда двигаться
                        row = -1;
                    } else {
                        column = 0;
                    }
                } else if (column < 0) {
                    --row;
                    if (row < 0) {
                        // в крайнем положении - больше некуда двигаться
                        row = -1;
                    } else {
                        column = maxC;
                    }
                }
            } else {
                row += dr;
                if (row > maxR || row < 0) {
                    row = -1;
                }
            }
            return new int[]{row, column};
        }

        private int[] moveBoundary(int startRow, int endRow, int startColumn, int endColumn) {
            for (int r = endRow, c = endColumn; r != startRow || c != startColumn; r -= dr, c -= dc) {
                if (isCellFocusable(r, c)) {
                    return new int[] {r, c};
                }
            }
            if (isCellFocusable(startRow, startColumn)) {
                return new int[] {startRow, startColumn};
            }
            return new int[] {-1, -1};
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        if (rowIndex != -1 && colIndex != -1) {
            ClientPropertyDraw cellProperty = getProperty(rowIndex, colIndex);
            if (cellProperty!=null && !cellProperty.echoSymbols) {
                Object value = getValueAt(rowIndex, colIndex);
                if (value != null) {
                    if (value instanceof Double) {
                        value = (double) Math.round(((Double) value) * 1000) / 1000;
                    }

                    String formattedValue;
                    try {
                        formattedValue = cellProperty.formatString(value);
                    } catch (ParseException e1) {
                        formattedValue = String.valueOf(value);
                    }

                    if (!BaseUtils.isRedundantString(formattedValue)) {
                        return SwingUtils.toMultilineHtml(formattedValue, createToolTip().getFont());
                    }
                }
            }
        }
        return null;
    }

    protected boolean isLocationInExpandControl(TreeUI ui, TreePath path, int mouseX, int mouseY) {
        if (ui instanceof BasicTreeUI) {
            try {
                Method declaredMethod = BasicTreeUI.class.getDeclaredMethod("isLocationInExpandControl", TreePath.class, int.class, int.class);
                declaredMethod.setAccessible(true);
                Object result = declaredMethod.invoke(ui, path, mouseX, mouseY);
                if (result != null) {
                    return (Boolean) result;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return false;
    }

    public Dimension getMaxPreferredSize(Dimension preferredSize) {
        return gridPropertyTable.getMaxPreferredSize(preferredSize);
    }

    public boolean isCurrentPathExpanded() {
        return isExpanded(currentTreePath);
    }

    private Integer hierarchicalUserWidth = null;
    private final Map<ClientPropertyDraw, Integer> userWidths = MapFact.mAddRemoveMap();
    private final GridPropertyTable gridPropertyTable = new GridPropertyTable() {
        public void setUserWidth(ClientPropertyDraw property, Integer value) {
            userWidths.put(property, value);
        }

        public Integer getUserWidth(ClientPropertyDraw property) {
            return userWidths.get(property);
        }

        @Override
        public ClientPropertyDraw getColumnPropertyDraw(int i) {
            assert i < getColumnsCount() - 1; // уже вычли фиксированную колонку
            return model.columnProperties.get(i);
        }

        @Override
        public TableColumn getColumnDraw(int i) {
            if(i == 0)
                return hierarchicalColumn;
            return columnsMap.get(getColumnPropertyDraw(i-1));
        }

        @Override
        public JTable getTable() {
            return TreeGroupTable.this;
        }

        public int getColumnsCount() {
            return 1 + model.columnProperties.size();
        }

        @Override
        protected boolean isColumnFlex(int i) {
            if(i == 0)
                return true;
            return super.isColumnFlex(i - 1);
        }

        @Override
        protected void setUserWidth(int i, int width) {
            if(i==0) {
                hierarchicalUserWidth = width;
                return;
            }
            super.setUserWidth(i - 1, width);
        }

        @Override
        protected Integer getUserWidth(int i) {
            if(i==0)
                return hierarchicalUserWidth;
            return super.getUserWidth(i - 1);
        }

        @Override
        protected int getColumnBaseWidth(int i) {
            if(i==0)
                return hierarchicalWidth;
            return super.getColumnBaseWidth(i - 1);
        }
    };
}
