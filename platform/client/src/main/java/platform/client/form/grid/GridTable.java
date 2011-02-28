package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormTable;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellTableInterface;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.queries.QueryView;
import platform.client.form.sort.GridHeaderMouseListener;
import platform.client.form.sort.GridHeaderRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;
import platform.interop.Order;
import platform.interop.Scroll;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public abstract class GridTable extends ClientFormTable
        implements CellTableInterface {

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

    private final GroupObjectLogicsSupplier logicsSupplier;

    // пока пусть GridTable напрямую общается с формой, а не через Controller, так как ей много о чем надо с ней говорить, а Controller будет просто бюрократию создавать
    private final ClientFormController form;
    private boolean tabVertical = false;

    private int viewMoveInterval = 0;
    private final GridView gridView;

    public GridTable(GridView gridView, GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform) {
        super(new GridTableModel());
        this.gridView = gridView;

        setAutoCreateColumnsFromModel(false);

        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        model = (GridTableModel) getModel();

        logicsSupplier = ilogicsSupplier;
        form = iform;

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                changeCurrentObject();
            }
        });

        final JTableHeader header = getTableHeader();
        header.setDefaultRenderer(new GridHeaderRenderer(header.getDefaultRenderer()) {
//        header.setDefaultRenderer(new GridHeaderRenderer.MultiLineHeaderRenderer(header.getDefaultRenderer()) {

            protected Boolean getSortDirection(int column) {
                return GridTable.this.getSortDirection(column);
            }

            @Override
            public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                model.getColumnProperty(column).design.designHeader(comp);
                return comp;
            }
        });

        header.addMouseListener(new GridHeaderMouseListener() {
            protected Boolean getSortDirection(int column) {
                return GridTable.this.getSortDirection(column);
            }

            protected TableColumnModel getColumnModel() {
                return GridTable.this.getColumnModel();
            }

            protected void changeOrder(int column, Order modiType) {

                try {
                    changeGridOrder(model.getColumnProperty(column), column, modiType);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка изменении сортировки", e);
                }

                header.repaint();
            }
        });

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
                        form.changePageSize(logicsSupplier.getGroupObject(), newPageSize);
                        pageSize = newPageSize;
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении размера страницы", e);
                    }
                }
            }
        });

        if (form.isDialogMode()) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        form.okPressed();
                    }
                }
            });
        }

        initializeActionMap();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        Object value = (rowIndex != -1 && colIndex != -1) ? getValueAt(rowIndex, colIndex) : null;
        return (value != null) ? String.valueOf(value).trim() : null;
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

        ActionMap actionMap = getActionMap();

        actionMap.put("selectNextColumnCell", nextAction);
        actionMap.put("selectPreviousColumnCell", prevAction);

        // set left and right actions
        actionMap.put("selectNextColumn", nextAction);
        actionMap.put("selectPreviousColumn", prevAction);
        // set tab and shift-tab actions
        actionMap.put("selectNextColumnCell", nextAction);
        actionMap.put("selectPreviousColumnCell", prevAction);
        // set top and end actions
        actionMap.put("selectFirstColumn", firstAction);
        actionMap.put("selectLastColumn", lastAction);

        //имитируем продвижение фокуса вперёд, если изначально попадаем на нефокусную ячейку
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                moveToFocusableCellIfNeeded();
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                moveToFocusableCellIfNeeded();
            }
        });

        this.moveToNextCellAction = nextAction;
    }

    int getID() {
        return logicsSupplier.getGroupObject().getID();
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
        model.update(logicsSupplier.getGroupObject(), properties, rowKeys, columnKeys, captions, values, rowHighlights, cellHighlights);

        refreshColumnModel();
        adjustSelection();

        setPreferredScrollableViewportSize(getPreferredSize());
        // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
        // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
//        setSize(getWidth(), getRowHeight() * getRowCount());
        if (logicsSupplier.getGroupObject().tableRowsCount >= 0) {
            int count = logicsSupplier.getGroupObject().tableRowsCount == 0 ? getRowCount() : logicsSupplier.getGroupObject().tableRowsCount;
            int height = count * (getRowHeight() + 1) + getTableHeader().getPreferredSize().height;
            gridView.pane.setMinimumSize(new Dimension(getMinimumSize().width, height));
            gridView.pane.setPreferredSize(new Dimension(getPreferredSize().width, height + 2));
            gridView.pane.setMaximumSize(new Dimension(getMaximumSize().width, height + 5));
        }
    }

    public void changeCurrentObject() {
        final ClientGroupObjectValue changeObject = getSelectedObject();
        if (changeObject != null) {
            SwingUtils.invokeLaterSingleAction(logicsSupplier.getGroupObject().getActionID()
                    , new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ClientGroupObjectValue newCurrentObject = getSelectedObject();
                                if (changeObject.equals(newCurrentObject)) {
                                    selectObject(newCurrentObject);
                                    form.changeGroupObject(logicsSupplier.getGroupObject(), getSelectedObject());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Ошибка при изменении текущего объекта", e);
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

            rowHeight = Math.max(rowHeight, cell.getPreferredHeight(this));

            hasFocusableCells |= cell.focusable == null || cell.focusable;

            boolean samePropAsPrevious = i != 0 && cell == model.getColumnProperty(i - 1);
            final int index = i;
            if (!samePropAsPrevious && cell.editKey != null) {
                form.getComponent().addKeyBinding(cell.editKey, cell.getKeyBindingGroup(), new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        int leadRow = getSelectionModel().getLeadSelectionIndex();
                        if (leadRow != -1 && !isEditing()) {
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
        if (colSel == -1) {
            isInternalNavigating = true;
            changeSelection(rowNumber, 0, false, false);
            isInternalNavigating = false;
            moveToFocusableCellIfNeeded();
        } else {
            getSelectionModel().setLeadSelectionIndex(rowNumber);
        }

        scrollRectToVisible(getCellRect(rowNumber, (colSel == -1) ? 0 : colSel, true));
    }

    public void setRowKeys(List<ClientGroupObjectValue> irowKeys) {
        int oldIndex = rowKeys.indexOf(currentObject);
        int newIndex = irowKeys.indexOf(currentObject);

        rowKeys = irowKeys;

        if (oldIndex != -1 && newIndex != -1) {
            viewMoveInterval = newIndex - oldIndex;
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

    public Object convertValueFromString(String value, int row, int column) {
        Object parsedValue;
        try {
            parsedValue = model.getColumnProperty(column).parseString(getForm(), value);
        } catch (ParseException pe) {
            return null;
        }
        return parsedValue;
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
        try {
            // Отдельно обработаем CTRL + HOME и CTRL + END
            if (ks.equals(KeyStrokes.getCtrlHome())) {
                form.changeGroupObject(logicsSupplier.getGroupObject(), Scroll.HOME);
                return true;
            }

            if (ks.equals(KeyStrokes.getCtrlEnd())) {
                form.changeGroupObject(logicsSupplier.getGroupObject(), Scroll.END);
                return true;
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Ошибка при переходе на запись", ioe);
        }


        boolean isReadOnlyDialog = form.isDialogMode() && form.isReadOnlyMode();
        if (isReadOnlyDialog && ks.equals(KeyStrokes.getApplyKeyStroke(isReadOnlyDialog))) {
            return false;
        }

        if (condition == WHEN_FOCUSED && gridView.hasActiveFilter() && ks.equals(KeyStrokes.getRemoveFiltersKeyStroke())) {
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

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == e.getLastRow() && e.getColumn() != -1) {
            int row = e.getFirstRow();
            int col = e.getColumn();
            Object value = model.getValueAt(row, col);

            try {
                form.changePropertyDraw(model.getColumnProperty(col), value, multyChange, model.getColumnKey(col));
            } catch (IOException ioe) {
                throw new RuntimeException("Ошибка при изменении значения свойства", ioe);
            }
        }
    }

    private boolean isCellFocusable(int row, int col) {
        //вообще говоря нужно скорректировать индексы к модели, но не актуально
        return model.isCellFocusable(row, col);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (isInternalNavigating || isCellFocusable(rowIndex, columnIndex)) {
            super.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
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
            SwingUtils.stopSingleAction(logicsSupplier.getGroupObject().getActionID(), false);
            return true;
        } else if (editEvent instanceof KeyEvent && cellEditor != null) {
            if (!cellEditor.editPerformed && cellEditor.isCellEditable(editEvent)) {
                gridView.quickEditFilter();
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
        if (properties.indexOf(property) == -1) {
            List<ClientPropertyDraw> cells = logicsSupplier.getPropertyDraws();

            // конечно кривовато определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ind = cells.indexOf(property), ins = 0;

            Iterator<ClientPropertyDraw> icp = properties.iterator();
            while (icp.hasNext() && cells.indexOf(icp.next()) < ind) {
                ins++;
            }

            properties.add(ins, property);
            return true;
        } else
            return false;
    }

    public boolean removeProperty(ClientPropertyDraw property) {
        if (properties.remove(property)) {
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

    public Object getSelectedValue(ClientPropertyDraw property) {
        return getSelectedValue(getMinPropertyIndex(property));
    }

    private Object getSelectedValue(int col) {
        int row = getSelectedRow();
        if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount()) {
            return getValueAt(row, col);
        } else {
            return null;
        }
    }

    public Object getValue(ClientPropertyDraw property, int row) {
        int col = getMinPropertyIndex(property);
        if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount()) {
            return model.getValueAt(row, col);
        } else {
            return null;
        }
    }

    public ClientGroupObjectValue getSelectedObject() {
        int rowIndex = convertRowIndexToModel(getSelectedRow());
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            return null;
        }

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

    public ClientPropertyDraw getProperty(int col) {
        return model.getColumnProperty(col);
    }

    public boolean isCellHighlighted(int row, int column) {
        return model.isCellHighlighted(row, column);
    }

    public Color getHighlightColor(int row, int column) {
        return model.getHighlightColor(row, column);
    }

    private final List<Integer> orders = new ArrayList<Integer>();
    private final List<Boolean> orderDirections = new ArrayList<Boolean>();

    public void changeGridOrder(ClientPropertyDraw property, int col, Order modiType) throws IOException {
        ClientGroupObjectValue columnKey = new ClientGroupObjectValue();

        if (col >= 0 && col < model.getColumnCount()) {
            columnKey = model.getColumnKey(col);
            int ordNum;
            switch (modiType) {
                case REPLACE:
                    orders.clear();
                    orderDirections.clear();

                    orders.add(col);
                    orderDirections.add(true);
                    break;
                case ADD:
                    orders.add(col);
                    orderDirections.add(true);
                    break;
                case DIR:
                    ordNum = orders.indexOf(col);
                    orderDirections.set(ordNum, !orderDirections.get(ordNum));
                    break;
                case REMOVE:
                    ordNum = orders.indexOf(col);
                    orders.remove(ordNum);
                    orderDirections.remove(ordNum);
                    break;
            }
            tableHeader.resizeAndRepaint();
        }

        form.changeOrder(property, modiType, columnKey);
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        changeGridOrder(property, getMinPropertyIndex(property), modiType);
    }

    public int getMinPropertyIndex(ClientPropertyDraw property) {
        return model.getMinPropertyIndex(property);
    }

    private Boolean getSortDirection(int column) {
        int ordNum = orders.indexOf(column);
        return (ordNum != -1) ? orderDirections.get(ordNum) : null;
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
            //changeSelection(num / getColumnCount(), num % getColumnCount(), false, false);
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
            ClientPropertyDraw cellView = model.getColumnProperty(modelIndex);
            String toolTip = !BaseUtils.isRedundantString(cellView.toolTip) ? cellView.toolTip : (String) columnModel.getColumn(index).getHeaderValue();
            toolTip += " (sID: " + cellView.getSID() + ")";
            return toolTip;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(columnModel.getTotalColumnWidth(), 34);
        }
    }
}