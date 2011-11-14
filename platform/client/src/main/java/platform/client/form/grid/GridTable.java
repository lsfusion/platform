package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormTable;
import platform.client.form.GroupObjectController;
import platform.client.form.cell.CellTableInterface;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.grid.groupchange.GroupChangeAction;
import platform.client.form.queries.QueryView;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.form.sort.MultiLineHeaderRenderer;
import platform.client.form.sort.TableSortableHeaderManager;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientDateClass;
import platform.interop.KeyStrokes;
import platform.interop.Order;
import platform.interop.Scroll;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static java.lang.Math.max;

public abstract class GridTable extends ClientFormTable
        implements CellTableInterface {

    public static final String GOTO_LAST_ACTION = "gotoLastRow";
    public static final String GOTO_FIRST_ACTION = "gotoFirstRow";
    public static final String GROUP_CORRECTION_ACTION = "groupPropertyCorrection";

    private final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();

    private List<ClientGroupObjectValue> rowKeys = new ArrayList<ClientGroupObjectValue>();
    private Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> captions = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientGroupObjectValue, Object> rowHighlights = new HashMap<ClientGroupObjectValue, Object>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellHighlights = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    private ClientGroupObjectValue currentObject;

    private final GridTableModel model;

    private Action moveToNextCellAction = null;

    private boolean multyChange = false;

    //нужно для отключения поиска свободных мест при навигации по таблице
    private boolean hasFocusableCells;

    private boolean isInternalNavigating = false;

    private boolean isLayouting;

    private final GroupObjectController groupObjectController;

    // пока пусть GridTable напрямую общается с формой, а не через Controller, так как ей много о чем надо с ней говорить, а Controller будет просто бюрократию создавать
    private final ClientFormController form;
    private boolean tabVertical = false;

    private int viewMoveInterval = 0;
    private ClientGroupObject groupObject;
    private TableSortableHeaderManager<Pair<ClientPropertyDraw, ClientGroupObjectValue>> sortableHeaderManager;

    //для вдавливаемости кнопок
    private int pressedCellRow = -1;
    private int pressedCellColumn = -1;
    private int previousSelectedRow = 0;

    private GridSelectionController selectionController;
    private KeyController keyController = new KeyController(this);

    public GridTable(GroupObjectController igroupObjectController, ClientFormController iform) {
        super(new GridTableModel());

        setAutoCreateColumnsFromModel(false);

        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        model = getModel();

        groupObjectController = igroupObjectController;
        form = iform;
        groupObject = groupObjectController.getGroupObject();
        selectionController = new GridSelectionController(this);

        sortableHeaderManager = new TableSortableHeaderManager<Pair<ClientPropertyDraw, ClientGroupObjectValue>>(this) {
            protected void orderChanged(Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey, Order modiType) {
                GridTable.this.orderChanged(columnKey, modiType);
            }

            @Override
            protected Pair<ClientPropertyDraw, ClientGroupObjectValue> getColumnKey(int column) {
                return new Pair<ClientPropertyDraw, ClientGroupObjectValue>(model.getColumnProperty(column), model.getColumnKey(column));
            }
        };

        tableHeader.setDefaultRenderer(new MultiLineHeaderRenderer(tableHeader.getDefaultRenderer(), sortableHeaderManager) {
            @Override
            public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                model.getColumnProperty(column).design.designHeader(comp);
                return comp;
            }
        });
        tableHeader.addMouseListener(sortableHeaderManager);

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

        addComponentListener(new ComponentAdapter() {
            private int pageSize = 50;

            public void componentResized(ComponentEvent ce) {
                //баг с прорисовкой хэдера

                // Listener срабатывает в самом начале, когда компонент еще не расположен
                // В таком случае нет смысла вызывать изменение pageSize
                if (getParent().getHeight() == 0) {
                    return;
                }

                int newPageSize = getParent().getHeight() / getRowHeight() + 1;
                if (newPageSize != pageSize) {
                    try {
                        form.changePageSize(groupObject, newPageSize);
                        pageSize = newPageSize;
                    } catch (IOException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.page.size"), e);
                    }
                }
            }
        });

        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}

            public void keyPressed(KeyEvent e) {
                if (KeyEvent.getKeyText(e.getKeyCode()).equals("Shift") && getSelectedObject() != null) {
                    if (!keyController.isRecording)
                        selectionController.recordingStarted(getSelectedColumn());
                    keyController.startRecording(getSelectedRow());
                }
            }

            public void keyReleased(KeyEvent e) {
                if (KeyEvent.getKeyText(e.getKeyCode()).equals("Shift")) {
                    keyController.stopRecording();
                    selectionController.recordingStopped();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (getSelectedRow() == previousSelectedRow) {
                    pressedCellColumn = columnAtPoint(e.getPoint());
                    pressedCellRow = rowAtPoint(e.getPoint());
                    repaint();
                }
                previousSelectedRow = getSelectedRow();

                if (getSelectedColumn() != -1) {
                    if (MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Shift")) {
                        if (!keyController.isRecording) {//пока кривовато работает
                            keyController.startRecording(getSelectedRow());
                            selectionController.recordingStarted(getSelectedColumn());
                        }
                        keyController.completeRecording(getSelectedRow());
                        selectionController.submitShiftSelection(keyController.getValues());
                    } else {
                        selectionController.mousePressed(getSelectedColumn());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int column = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                if (row != -1 && column != -1 && pressedCellColumn == column && pressedCellRow == row && getProperty(row, column).getRendererComponent() instanceof ActionPropertyRenderer) {
                    editCellAt(row, column);
                }
                pressedCellRow = -1;
                pressedCellColumn = -1;
                previousSelectedRow = getSelectedRow();

                if (!MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Shift"))
                    selectionController.mouseReleased();

                repaint();
            }
        });

        if (form.isModal() && form.isReadOnlyMode()) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        form.okPressed();
                    }
                }
            });
        }

        //имитируем продвижение фокуса вперёд, если изначально попадаем на нефокусную ячейку
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                moveToFocusableCellIfNeeded();
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
//                if (!e.getValueIsAdjusting()) {
                    changeCurrentObject();
//                }
                moveToFocusableCellIfNeeded();
            }
        });

        initializeActionMap();
    }

     @Override
     public boolean isEditOnSingleClick(int row, int column){
         return getProperty(row, column).editOnSingleClick;
    }

    private void orderChanged(Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey, Order modiType) {
        try {
            form.changeOrder(columnKey.first, modiType, columnKey.second);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.sorting"), e);
        }

        tableHeader.repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        Object value = (rowIndex != -1 && colIndex != -1) ? getValueAt(rowIndex, colIndex) : null;
        if (value instanceof Date) {
            value = Main.formatDate(value);
        }
        return (value != null) ? SwingUtils.toMultilineHtml(BaseUtils.rtrim(String.valueOf(value)), createToolTip().getFont()) : null;
    }

    private void initializeActionMap() {
        final Action oldNextAction = getActionMap().get("selectNextColumnCell");
        final Action oldPrevAction = getActionMap().get("selectPreviousColumnCell");
        final Action oldFirstAction = getActionMap().get("selectFirstColumn");
        final Action oldLastAction = getActionMap().get("selectLastColumn");

        final Action nextAction = new GoToCellAction(true);
        final Action prevAction = new GoToCellAction(false);
        final Action firstAction = new GoToLastCellAction(oldFirstAction, oldNextAction);
        final Action lastAction = new GoToLastCellAction(oldLastAction, oldPrevAction);

        moveToNextCellAction = nextAction;

        ActionMap actionMap = getActionMap();
        // set left and right actions
        actionMap.put("selectNextColumn", nextAction);
        actionMap.put("selectPreviousColumn", prevAction);
        // set tab and shift-tab actions
        actionMap.put("selectNextColumnCell", nextAction);
        actionMap.put("selectPreviousColumnCell", prevAction);
        // set top and end actions
        actionMap.put("selectFirstColumn", firstAction);
        actionMap.put("selectLastColumn", lastAction);

        actionMap.put(GOTO_FIRST_ACTION, new ScrollToEndAction(Scroll.HOME));
        actionMap.put(GOTO_LAST_ACTION, new ScrollToEndAction(Scroll.END));
        actionMap.put(GROUP_CORRECTION_ACTION, new GroupChangeAction(this));


        InputMap inputMap = getInputMap();
        inputMap.put(KeyStrokes.getCtrlHome(), GOTO_FIRST_ACTION);
        inputMap.put(KeyStrokes.getCtrlEnd(), GOTO_LAST_ACTION);
        inputMap.put(KeyStrokes.getGroupCorrectionKeyStroke(), GROUP_CORRECTION_ACTION);
    }

    int getID() {
        return groupObject.getID();
    }

    @Override
    public int hashCode() {
        return getID();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GridTable && ((GridTable) o).getID() == this.getID();
    }

    public void updateTable() {
        model.update(groupObject, properties, rowKeys, columnKeys, captions, values, rowHighlights, cellHighlights);

        refreshColumnModel();
        if (viewMoveInterval != 0)
            selectionController.keysChanged(viewMoveInterval < 0);
        adjustSelection();

        setPreferredScrollableViewportSize(getPreferredSize());
        // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
        // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
//        setSize(getWidth(), getRowHeight() * getRowCount());
        if (groupObject.tableRowsCount >= 0) {
            int count = groupObject.tableRowsCount == 0 ? getRowCount() : groupObject.tableRowsCount;
            int height = count * (getRowHeight() + 1) + getTableHeader().getPreferredSize().height;
            groupObjectController.getGridView().pane.setMinimumSize(new Dimension(getMinimumSize().width, height));
            groupObjectController.getGridView().pane.setPreferredSize(new Dimension(getPreferredSize().width, height + 2));
            groupObjectController.getGridView().pane.setMaximumSize(new Dimension(getMaximumSize().width, height + 5));
        }

        if (groupObject.grid.minimumSize != null)
            groupObjectController.getGridView().pane.setMinimumSize(
                    SwingUtils.getOverridedSize(groupObjectController.getGridView().pane.getMinimumSize(), groupObject.grid.minimumSize));

        if (groupObject.grid.preferredSize != null)
            groupObjectController.getGridView().pane.setPreferredSize(
                    SwingUtils.getOverridedSize(groupObjectController.getGridView().pane.getPreferredSize(), groupObject.grid.preferredSize));

        if (groupObject.grid.maximumSize != null)
            groupObjectController.getGridView().pane.setMaximumSize(
                    SwingUtils.getOverridedSize(groupObjectController.getGridView().pane.getMaximumSize(), groupObject.grid.maximumSize));

        previousSelectedRow = getSelectedRow();
    }

    public void changeCurrentObject() {
        final ClientGroupObjectValue changeObject = getSelectedObject();
        if (changeObject != null) {

//            if (!isInternalNavigating) {
//                int row = getSelectedRow();
//                JTable editedTable = SwingUtils.commitCurrentEditing();
//                if (editedTable != null && this != editedTable) { //если нужно, завершаем редактирование свойства, вынесенного в панель
//                    selectRow(row);
//                    requestFocusInWindow();
//                }
//            }

            SwingUtils.invokeLaterSingleAction(groupObject.getActionID()
                    , new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ClientGroupObjectValue newCurrentObject = getSelectedObject();
                                if (changeObject.equals(newCurrentObject)) {
                                    selectObject(newCurrentObject);
                                    form.changeGroupObject(groupObject, getSelectedObject());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.current.object"), e);
                            }
                        }
                    }, 50);
        }
    }

    private void refreshColumnModel() {
        TableColumnModel columnModel = getColumnModel();
        int newColumnCount = model.getColumnCount();
        int oldColumnCount = columnModel.getColumnCount();
        if (newColumnCount > oldColumnCount) {
            while (newColumnCount > columnModel.getColumnCount()) {
                addColumn(new TableColumn(columnModel.getColumnCount()));
            }
        } else {
            while (newColumnCount < columnModel.getColumnCount()) {
                removeColumn(columnModel.getColumn(columnModel.getColumnCount() - 1));
            }
        }

        int rowHeight = 0;
        hasFocusableCells = false;
        for (int i = 0; i < model.getColumnCount(); ++i) {
            ClientPropertyDraw cell = model.getColumnProperty(i);

            TableColumn column = getColumnModel().getColumn(i);
            if (newColumnCount != oldColumnCount) {
                if (autoResizeMode == JTable.AUTO_RESIZE_OFF)
                    column.setPreferredWidth(cell.getMinimumWidth(this));
                else
                    column.setPreferredWidth(cell.getPreferredWidth(this));
                column.setMinWidth(cell.getMinimumWidth(this));
                column.setMaxWidth(cell.getMaximumWidth(this));
            }
            column.setHeaderValue(model.getColumnName(i));

            rowHeight = max(rowHeight, cell.getPreferredHeight(this));

            hasFocusableCells |= cell.focusable == null || cell.focusable;

            boolean samePropAsPrevious = i != 0 && cell == model.getColumnProperty(i - 1);
            final int index = i;
            if (!samePropAsPrevious && cell.editKey != null) {
                form.getComponent().addKeyBinding(cell.editKey, cell.getKeyBindingGroup(), new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        int leadRow = getSelectionModel().getLeadSelectionIndex();
                        if (leadRow != -1 && !isEditing()) {
                            keyController.stopRecording();
                            editCellAt(leadRow, index);
                        }
                    }
                });
            }
        }

        if (model.getColumnCount() != 0) {
            setRowHeight(rowHeight);
            tableHeader.resizeAndRepaint();
            needToBeShown();
        } else {
            needToBeHidden();
        }
    }

    private void adjustSelection() {
        //надо сдвинуть ViewPort - иначе дергаться будет
        final Point viewPos = ((JViewport) getParent()).getViewPosition();
        final int dltpos = viewMoveInterval * getRowHeight();
        viewPos.y += dltpos;
        if (viewPos.y < 0) viewPos.y = 0;
        ((JViewport) getParent()).setViewPosition(viewPos);
        viewMoveInterval = 0;

        selectRow(rowKeys.indexOf(currentObject));
    }

    protected void selectRow(int rowNumber) {
        if (rowNumber < 0 || rowNumber > getRowCount()) {
            return;
        }

        final int colSel = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        // scrollRectToVisible обязательно должен идти до setLeadSelectionIndex
        // иначе, если объект за пределами текущего "окна", сработает JViewport.changeListener
        // и он изменит текущий объект на другой (firstRow или lastRow)
        scrollRectToVisible(getCellRect(rowNumber, (colSel == -1) ? 0 : colSel, true));

        if (colSel == -1) {
            isInternalNavigating = true;
            changeSelection(rowNumber, 0, false, false);
            isInternalNavigating = false;
            moveToFocusableCellIfNeeded();
        } else {
            getSelectionModel().setLeadSelectionIndex(rowNumber);
        }
    }

    public void setRowKeys(List<ClientGroupObjectValue> irowKeys) {
        int oldIndex = rowKeys.indexOf(currentObject);
        int newIndex = irowKeys.indexOf(currentObject);

        rowKeys = irowKeys;

        if (oldIndex != -1 && newIndex != -1) {
            viewMoveInterval = newIndex - oldIndex;
        }
        if (newIndex == -1) {
            selectionController.resetSelection();
            updateSelectionInfo();
        }
    }

    public void selectObject(ClientGroupObjectValue value) {
        if (rowKeys.contains(value)) {
            currentObject = value;
        }
    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
    }

    public List<ClientPropertyDraw> getVisibleProperties() {
        //возвращает все свойства, за исключеним тех, что формируют группы в колонки без единого значения
        List<ClientPropertyDraw> props = new ArrayList<ClientPropertyDraw>(properties);
        for (ClientPropertyDraw property : properties) {
            if (getModel().getPropertyIndex(property, null) == -1) {
                props.remove(property);
            }
        }
        return props;
    }

    public List<ClientGroupObjectValue> getRowKeys() {
        return rowKeys;
    }

    private boolean fitWidth() {
        int minWidth = 0;
        TableColumnModel columnModel = getColumnModel();

        for (int i = 0; i < getColumnCount(); i++) {
            if (autoResizeMode == JTable.AUTO_RESIZE_OFF) {
                minWidth += columnModel.getColumn(i).getWidth();
            } else {
                minWidth += columnModel.getColumn(i).getMinWidth();
            }
        }

        // тут надо смотреть pane, а не саму table
        return (minWidth < getParent().getWidth());
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return fitWidth();
    }

    @Override
    public void doLayout() {
        int newAutoResizeMode = fitWidth()
                ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
                : JTable.AUTO_RESIZE_OFF;
        if (newAutoResizeMode != autoResizeMode) {
            autoResizeMode = newAutoResizeMode;
            setAutoResizeMode(newAutoResizeMode);

            if (newAutoResizeMode == JTable.AUTO_RESIZE_OFF) {
                setPreferredColumnWidthsAsMinWidth();
            } else {
                resetPreferredColumnWidths();
            }
        }
        super.doLayout();
    }

    @Override
    public GridTableModel getModel() {
        return (GridTableModel) super.getModel();
    }

    private void setPreferredColumnWidthsAsMinWidth() {
        for (int i = 0; i < model.getColumnCount(); ++i) {
            getColumnModel().getColumn(i).setPreferredWidth(getColumnModel().getColumn(i).getMinWidth());
        }
    }

    private void resetPreferredColumnWidths() {
        for (int i = 0; i < model.getColumnCount(); ++i) {
            ClientPropertyDraw cell = model.getColumnProperty(i);
            getColumnModel().getColumn(i).setPreferredWidth(cell.getPreferredWidth(this));
        }
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean isReadOnlyDialog = form.isModal() && form.isReadOnlyMode();
        if (isReadOnlyDialog && ks.equals(KeyStrokes.getApplyKeyStroke(isReadOnlyDialog))) {
            return false;
        }

        if (condition == WHEN_FOCUSED && groupObjectController.hasActiveFilter() && ks.equals(KeyStrokes.getRemoveFiltersKeyStroke())) {
            Action removeAllAction = getActionMap().get(QueryView.REMOVE_ALL_ACTION);
            if (removeAllAction != null) {
                return SwingUtilities.notifyAction(removeAllAction, ks, e, this, e.getModifiers());
            }
        }

        if (ks.equals(KeyStrokes.getEnter())) {
            commitEditing();
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }

    private void commitEditing() {
        SwingUtils.commitEditing(this);
    }

    public ClientFormController getForm() {
        return form;
    }

    public ClientPropertyDraw getCurrentProperty() {
        ClientPropertyDraw selectedProperty = getSelectedProperty();
        return selectedProperty != null
                ? selectedProperty
                : model.getColumnCount() > 0
                ? model.getColumnProperty(0)
                : null;
    }

    public void changePropertyDraw(Object value, int row, int col, boolean multyChange, boolean aggValue) {
        try {
            form.changePropertyDraw(model.getColumnProperty(col), model.getColumnKey(col), value, multyChange, aggValue);
        } catch (IOException ioe) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.property.value"), ioe);
        }
    }

    public void writeSelectedValue(String value) {
        if (isReadOnly())
            return;

        int row = getSelectionModel().getLeadSelectionIndex();
        int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        Object oValue;
        try {
            oValue = model.getColumnProperty(column).parseString(getForm(), model.getColumnKey(column), value, isDataChanging());
        } catch (ParseException e) {
            oValue = null;
        }
        if (oValue != null)
            changePropertyDraw(oValue, row, column, false, false);
    }

    public void pasteTable(List<List<String>> table) {
        if (isReadOnly())
            return;

        boolean singleV = selectionController.hasSingleSelection();
        int selectedColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (selectedColumn == -1)
            return;
        int tableColumns = 0;
        if (!table.isEmpty()) {
            tableColumns = table.get(0).size();
        }
        boolean singleC = table.size() == 1 && tableColumns == 1;
        if (!singleV || !singleC) {
            int answer = SwingUtils.showConfirmDialog(null, ClientResourceBundle.getString("form.grid.sure.to.paste.multivalue"), "", JOptionPane.QUESTION_MESSAGE, 1);
            if (answer == JOptionPane.NO_OPTION)
                return;
        }

        try {
            if (singleV) {
                int columnsToInsert = Math.min(tableColumns, getColumnCount() - selectedColumn);

                List<ClientPropertyDraw> propertyList = new ArrayList<ClientPropertyDraw>();
                for (int i = 0; i < columnsToInsert; i++) {
                    ClientPropertyDraw propertyDraw = model.getColumnProperty(selectedColumn + i);
                    propertyList.add(propertyDraw);
                }

                List<List<Object>> pasteTable = new ArrayList<List<Object>>();
                for (List<String> row : table) {
                    List<Object> pasteTableRow = new ArrayList<Object>();
                    int itemIndex = -1;
                    for (String item : row) {
                        itemIndex++;
                        if (itemIndex <= columnsToInsert - 1) {
                            ClientPropertyDraw property = propertyList.get(itemIndex);
                            try {
                                pasteTableRow.add(item == null ? null : property.parseString(getForm(), model.getColumnKey(itemIndex), item, isDataChanging()));
                            } catch (ParseException e) {
                                pasteTableRow.add(null);
                            }
                        }
                    }
                    pasteTable.add(pasteTableRow);
                }
                form.pasteExternalTable(propertyList, pasteTable);
            } else {
                form.pasteMulticellValue(selectionController.getSelectedCells(), table.get(0).get(0));
                selectionController.resetSelection();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isReadOnly() {
        return form.isReadOnlyMode() && isDataChanging();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == e.getLastRow() && e.getColumn() != -1) {
            int row = e.getFirstRow();
            int col = e.getColumn();
            changePropertyDraw(model.getValueAt(row, col), row, col, multyChange, true);
        }
    }

    private boolean isCellFocusable(int row, int col) {
        //вообще говоря нужно скорректировать индексы к модели, но не актуально
        return model.isCellFocusable(row, col);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (isInternalNavigating || isCellFocusable(rowIndex, columnIndex)) {
            if (!properties.isEmpty()) {
                selectionController.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
            super.changeSelection(rowIndex, columnIndex, toggle, extend);
            updateSelectionInfo();
            repaint();
        }
    }

    private void updateSelectionInfo() {
        int quantity = selectionController.getQuantity();
        if (quantity > 1) {
            int numbers = selectionController.getNumbersQuantity();
            if (numbers > 1) {
                Double sum = selectionController.getSum();
                NumberFormat format = java.text.NumberFormat.getNumberInstance();
                groupObjectController.updateSelectionInfo(quantity, format.format(sum), format.format(sum / numbers));
                return;
            }
        }
        groupObjectController.updateSelectionInfo(quantity, null ,null);
    }

    public String getSelectedTable() throws ParseException {
        return selectionController.getSelectedTableString();
    }

    /**
     * Переопределено, чтобы учесть групповую корректировку...
     * <br><br>
     * <p/>
     * {@inheritDoc}
     *
     * @see GridTableModel#setValueAt(Object, int, int, boolean)
     */
    public void setValueAt(Object aValue, int row, int column) {
        getModel().setValueAt(aValue, convertRowIndexToModel(row),
                convertColumnIndexToModel(column), multyChange);
    }

    public boolean editCellAt(int row, int column, EventObject editEvent) {
        multyChange = KeyStrokes.isGroupCorrectionEvent(editEvent);

        ClientAbstractCellEditor cellEditor = getAbstractCellEditor(row, column);
        if (cellEditor != null) {
            cellEditor.editPerformed = false;
        }

        if (super.editCellAt(row, column, editEvent)) {
            //нужно для редактирования нефокусных ячеек
            isInternalNavigating = true;
            changeSelection(row, column, false, false);
            isInternalNavigating = false;
            SwingUtils.stopSingleAction(groupObject.getActionID(), false);
            return true;
        } else if (editEvent instanceof KeyEvent && cellEditor != null) {
            if (!cellEditor.editPerformed && cellEditor.isCellEditable(editEvent)) {
                groupObjectController.quickEditFilter();
            }
        }

        return false;
    }

    private ClientAbstractCellEditor getAbstractCellEditor(int row, int column) {
        TableCellEditor editor = getCellEditor(row, column);
        return editor instanceof ClientAbstractCellEditor
                ? (ClientAbstractCellEditor) editor
                : null;
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public boolean addProperty(final ClientPropertyDraw property) {
        if (!properties.contains(property)) {
            // конечно кривовато определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ins = BaseUtils.relativePosition(property, form.getPropertyDraws(), properties);
            properties.add(ins, property);
            selectionController.addProperty(property);
            return true;
        } else
            return false;
    }

    public boolean removeProperty(ClientPropertyDraw property) {
        if (properties.contains(property)) {
            selectionController.removeProperty(property);
            properties.remove(property);
            values.remove(property);
            captions.remove(property);
            columnKeys.remove(property);
            return true;
        }

        return false;
    }

    public void updateColumnCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        this.captions.put(property, captions);
    }

    public void updateCellHighlightValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellHighlights) {
        this.cellHighlights.put(property, cellHighlights);
    }

    public void setColumnValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        this.values.put(property, values);
    }

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys) {
        this.columnKeys.put(property, columnKeys);
    }

    public void updateRowHighlightValues(Map<ClientGroupObjectValue, Object> rowHighlights) {
        this.rowHighlights = rowHighlights;
    }

    public Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        return getSelectedValue(model.getPropertyIndex(property, columnKey));
    }

    private Object getSelectedValue(int col) {
        int row = getSelectedRow();
        if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount()) {
            return getValueAt(row, col);
        } else {
            return null;
        }
    }

    public ClientGroupObjectValue getSelectedObject() {
        int rowIndex = getSelectedRow();
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            return null;
        }
        rowIndex = convertRowIndexToModel(rowIndex);

        try {
            return rowKeys.get(rowIndex);
        } catch (IndexOutOfBoundsException be) {
            be.printStackTrace();
            return null;
        }
    }

    public ClientPropertyDraw getSelectedProperty() {
        int colView = getSelectedColumn();
        if (colView < 0 || colView >= getColumnCount()) {
            return null;
        }

        int colModel = convertColumnIndexToModel(colView);
        if (colModel < 0) {
            return null;
        }

        return model.getColumnProperty(colModel);
    }

    public boolean isDataChanging() {
        return true;
    }

    public boolean isPressed(int row, int column) {
        return pressedCellRow == row && pressedCellColumn == column;
    }

    public int getColumnCount() {
        return model.getColumnCount();
    }

    public ClientPropertyDraw getProperty(int row, int col) {
        return model.getColumnProperty(col);
    }

    public ClientGroupObjectValue getKey(int row, int col) {
        return model.getColumnKey(col);
    }

    public boolean isSelected(int row, int column) {
        return selectionController.isCellSelected(getProperty(row, column), rowKeys.get(row));
    }

    public boolean isCellHighlighted(int row, int column) {
        return model.isCellHighlighted(row, column);
    }

    public Color getHighlightColor(int row, int column) {
        return model.getHighlightColor(row, column);
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        int ind = getMinPropertyIndex(property);
        sortableHeaderManager.changeOrder(new Pair<ClientPropertyDraw, ClientGroupObjectValue>(property, ind == -1 ? new ClientGroupObjectValue() : model.getColumnKey(ind)), modiType);
    }

    public int getMinPropertyIndex(ClientPropertyDraw property) {
        return model.getPropertyIndex(property, null);
    }

    public GridTableModel getTableModel() {
        return model;
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new GridTableHeader(columnModel);
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        if (propertyDraw == null) {
            return;
        }

        int ind = getMinPropertyIndex(propertyDraw);
        if (ind != -1) {
            setColumnSelectionInterval(ind, ind);
        }
    }

    public void configureWheelScrolling(final JScrollPane pane) {
        assert pane.getViewport() == getParent();
        if (groupObject.pageSize != 0) {
            pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    int currRow = getSelectedRow();
                    if (currRow != -1) {
                        Rectangle viewRect = pane.getViewport().getViewRect();
                        int firstRow = rowAtPoint(new Point(0, viewRect.y + getRowHeight() - 1));
                        int lastRow = rowAtPoint(new Point(0, viewRect.y + viewRect.height - getRowHeight() + 1));

                        if (lastRow < firstRow) return;

                        if (isLayouting) {
                            selectRow(currRow);
                        } else {
                            if (currRow > lastRow) {
                                keyController.record(false, lastRow);
                                selectRow(lastRow);
                            } else if (currRow < firstRow) {
                                keyController.record(true, firstRow);
                                selectRow(firstRow);
                            }
                        }
                    }
                }
            });
        }
    }

    public void setLayouting(boolean isLayouting) {
        this.isLayouting = isLayouting;
    }

    private class GoToCellAction extends AbstractAction {
        private boolean isNext;

        public GoToCellAction(boolean isNext) {
            this.isNext = isNext;
        }

        private int moveNext(int row, int column, boolean isNext) {

            if (isNext) {
                if (tabVertical) {
                    row++;
                } else {
                    column++;
                }
            } else {
                if (tabVertical) {
                    row--;
                } else {
                    column--;
                }
            }
            int num;
            if (tabVertical) {
                num = column * getRowCount() + row;
            } else {
                num = row * getColumnCount() + column;
            }
            if (num < 0) {
                num += getColumnCount() * getRowCount();
            }
            if (num >= getColumnCount() * getRowCount()) {
                num = 0;
            }
            return num;
        }

        public void actionPerformed(ActionEvent e) {
            if (!hasFocusableCells || rowKeys.size() == 0) return;
            isInternalNavigating = true;

            int initRow = getSelectedRow();
            int initColumn = getSelectedColumn();

            int row = getSelectedRow();
            int column = getSelectedColumn();
            int oRow;
            int oColumn;
            if (row == -1 && column == -1) {
                changeSelection(0, 0, false, false);
            }
            do {
                int next = moveNext(row, column, isNext);

                oRow = row;
                oColumn = column;

                if (tabVertical) {
                    column = next / getRowCount();
                    row = next % getRowCount();
                } else {
                    row = next / getColumnCount();
                    column = next % getColumnCount();
                }
                if (((row == 0 && column == 0 && isNext) || (row == getRowCount() - 1 && column == getColumnCount() - 1 && (!isNext)))
                        && isCellFocusable(initRow, initColumn)) {
                    row = initRow;
                    column = initColumn;
                    break;
                }
            } while ((oRow != row || oColumn != column) && !isCellFocusable(row, column));

            commitEditing();
            changeSelection(row, column, false, false);
            isInternalNavigating = false;
        }
    }

    private class GoToLastCellAction extends AbstractAction {
        private Action oldMoveLastAction;
        private Action oldMoveAction;

        public GoToLastCellAction(Action oldMoveLastAction, Action oldMoveAction) {
            this.oldMoveLastAction = oldMoveLastAction;
            this.oldMoveAction = oldMoveAction;
        }

        public void actionPerformed(ActionEvent e) {
            if (!hasFocusableCells || rowKeys.size() == 0) return;
            isInternalNavigating = true;

            oldMoveLastAction.actionPerformed(e);

            int row = getSelectedRow();
            int column = getSelectedColumn();
            int oRow = row + 1;
            int oColumn = column + 1;
            while ((oRow != row || oColumn != column) && !isCellFocusable(row, column)) {
                oldMoveAction.actionPerformed(e);
                oRow = row;
                oColumn = column;

                row = getSelectedRow();
                column = getSelectedColumn();
            }

            isInternalNavigating = false;
        }
    }

    private class ScrollToEndAction extends AbstractAction {
        private final Scroll direction;

        private ScrollToEndAction(Scroll direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (groupObject.pageSize != 0) {
                    form.changeGroupObject(groupObject, direction);
                } else if (!rowKeys.isEmpty()) {
                    switch (direction) {
                        case HOME:
                            selectObject(rowKeys.get(0));
                            break;
                        case END:
                            selectObject(rowKeys.get(rowKeys.size() - 1));
                            break;
                    }
                    updateTable();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ClientResourceBundle.getString("errors.error.moving.to.the.node"), ioe);
            }
        }
    }

    private class GridTableHeader extends JTableHeader {
        public GridTableHeader(TableColumnModel columnModel) {
            super(columnModel);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int index = columnModel.getColumnIndexAtX(e.getPoint().x);
            if (index == -1) {
                return super.getToolTipText(e);
            }
            int modelIndex = columnModel.getColumn(index).getModelIndex();

            return model.getColumnProperty(modelIndex).getTooltipText((String) columnModel.getColumn(index).getHeaderValue());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(columnModel.getTotalColumnWidth(), 34);
        }
    }
}