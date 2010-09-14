package platform.client.form.grid;

import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormTable;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellTableInterface;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.sort.GridHeaderMouseListener;
import platform.client.form.sort.GridHeaderRenderer;
import platform.client.logics.*;
import platform.interop.Order;
import platform.interop.Scroll;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
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

    private final List<ClientPropertyDraw> columnProperties = new ArrayList<ClientPropertyDraw>();

    private List<ClientGroupObjectValue> rowKeys = new ArrayList<ClientGroupObjectValue>();
    private Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObject, List<ClientGroupObjectValue>>> propertyColumnKeys = new HashMap<ClientPropertyDraw, Map<ClientGroupObject, List<ClientGroupObjectValue>>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnDisplayValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private final Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

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

    public GridTable(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform) {
        super(new GridTableModel());

        setAutoCreateColumnsFromModel(false);

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
                    changeGridOrder(column, modiType);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка изменении сортировки", e);
                }

                header.repaint();
            }
        });

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

        adjustHeaderSize();
        addComponentListener(new ComponentAdapter() {
            private int pageSize = 50;

            public void componentResized(ComponentEvent ce) {
                //баг с прорисовкой хэдера
                adjustHeaderSize();

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

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (form.isDialogMode() && e.getClickCount() > 1) form.okPressed();
            }
        });

        initializeActionMap();
    }

    private void adjustHeaderSize() {
        tableHeader.setPreferredSize(new Dimension(columnModel.getTotalColumnWidth(), 34));
        tableHeader.resizeAndRepaint();
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

    // приходится давать доступ к rowKeys, так как контроллеру нужно заполнять значения колонок на основе ключей рядов
    public List<ClientGroupObjectValue> getRowKeys() {
        return rowKeys;
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
        commitEditing();

        model.update(columnProperties, rowKeys, columnKeys, columnDisplayValues, values);

        refreshColumnModel();
        changeCurrentObject();
        adjustSelection();

        // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
        // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
//        setSize(getWidth(), getRowHeight() * getRowCount());
    }

    public void changeCurrentObject() {
        final ClientGroupObjectValue changeObject = getSelectedObject();
        if (changeObject != null) {
            SwingUtils.invokeLaterSingleAction(logicsSupplier.getGroupObject().getActionID()
                    , new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                if (changeObject.equals(getSelectedObject())) {
                                    currentObject = getSelectedObject(); // нужно менять текущий выбранный объект для правильного скроллирования
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
                form.getComponent().addKeyBinding(cell.editKey, cell.getGroupObject(), new KeyAdapter() {
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
        if (newCurrentObjectIndex != -1) {
            selectRow(newCurrentObjectIndex);
            if (oldCurrentObjectIndex != -1 && newCurrentObjectIndex != oldCurrentObjectIndex) {
                final Point viewPos = ((JViewport) getParent()).getViewPosition();
                final int dltpos = (newCurrentObjectIndex - oldCurrentObjectIndex) * getRowHeight();
                viewPos.y += dltpos;
                if (viewPos.y < 0) viewPos.y = 0;
                ((JViewport) getParent()).setViewPosition(viewPos);
            }
        }
    }

    protected void selectRow(int rowNumber) {
        final int colSel = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (colSel == -1) {
            changeSelection(rowNumber, 0, false, false);
        } else {
            getSelectionModel().setLeadSelectionIndex(rowNumber);
        }

        scrollRectToVisible(getCellRect(rowNumber, (colSel == -1) ? 0 : colSel, true));
    }

    private int newCurrentObjectIndex = -1;
    private int oldCurrentObjectIndex = -1;

    public void setRowKeys(List<ClientGroupObjectValue> irowKeys) {
        oldCurrentObjectIndex = rowKeys.indexOf(currentObject);

        rowKeys = irowKeys;

        newCurrentObjectIndex = rowKeys.indexOf(currentObject);
    }

    public void setGridDisplayPropertiesValues(Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> pcolumnDisplayValues) {
        columnDisplayValues.putAll( pcolumnDisplayValues );
    }

    public void setColumnKeys(ClientPropertyDraw drawProperty, Map<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        if (groupColumnKeys == null || groupColumnKeys.isEmpty()) {
            return;
        }

        OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> keys = (OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>>) propertyColumnKeys.get(drawProperty);
        if (keys == null) {
            keys = new OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>>();
            //сразу вставляем все ключи, чтобы сохранить порядок
            for (ClientGroupObject columnGroup : drawProperty.columnGroupObjects) {
                keys.put(columnGroup, null);
            }

            propertyColumnKeys.put(drawProperty, keys);
        }

        keys.putAll(groupColumnKeys);

        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<ClientGroupObjectValue>();
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : keys.entrySet()) {
            List<ClientGroupObjectValue> groupObjectKeys = entry.getValue();

            if (propColumnKeys.size() == 0) {
                for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                    propColumnKeys.add(new ClientGroupObjectValue(groupObjectKey));
                }
            } else {
                List<ClientGroupObjectValue> newPropColumnKeys = new ArrayList<ClientGroupObjectValue>();
                for (ClientGroupObjectValue propColumnKey : propColumnKeys) {
                    for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                        newPropColumnKeys.add(new ClientGroupObjectValue(propColumnKey, groupObjectKey));
                    }
                }
                propColumnKeys = newPropColumnKeys;
            }
        }

        columnKeys.put(drawProperty, propColumnKeys);
    }

    public void selectObject(ClientGroupObjectValue value) {
        oldCurrentObjectIndex = getSelectionModel().getLeadSelectionIndex();
        newCurrentObjectIndex = rowKeys.indexOf(value);

        if (newCurrentObjectIndex != -1 && newCurrentObjectIndex != oldCurrentObjectIndex) {
            //Выставляем именно первую активную колонку, иначе фокус на таблице - вообще нереально увидеть
            selectRow(newCurrentObjectIndex);
        }
    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
    }

    public Object convertValueFromString(String value, int row, int column) {
        Object parsedValue = null;
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
            minWidth += columnModel.getColumn(i).getMinWidth();
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
        }

        super.doLayout();
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ae, int condition, boolean pressed) {
        try {
            // Отдельно обработаем CTRL + HOME и CTRL + END
            if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK))) {
                form.changeGroupObject(logicsSupplier.getGroupObject(), Scroll.HOME);
                return true;
            }

            if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK))) {
                form.changeGroupObject(logicsSupplier.getGroupObject(), Scroll.END);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при переходе на запись", e);
        }

        if (form.isDialogMode() && form.isReadOnlyMode() && ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))) {
            return false;
        }

        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))) {
            commitEditing();
        }

        return super.processKeyBinding(ks, ae, condition, pressed);
    }

    private void commitEditing() {
        if (isEditing()) {
            if (!getCellEditor().stopCellEditing()) {
                getCellEditor().cancelCellEditing();
            }
        }
    }

    public ClientFormController getForm() {
        return form;
    }

    public ClientPropertyDraw getCurrentProperty() {
        return getSelectedProperty();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == e.getLastRow() && e.getColumn() != -1) {
            int row = e.getFirstRow();
            int col = e.getColumn();
            Object value = model.getValueAt(row, col);

            try {
                ClientGroupObjectValue columnKey = model.getColumnKey(col);
                if (columnKey == null) {
                    form.changeProperty(model.getColumnProperty(col), value, multyChange);
                } else {
                    form.changePropertyDrawWithColumnKeys((ClientPropertyDraw)model.getColumnProperty(col), value, multyChange, columnKey);
                }
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

    public void changeSelectionInternal(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        isInternalNavigating = true;
        changeSelection(rowIndex, columnIndex, toggle, extend);
        isInternalNavigating = false;
    }

    public boolean editCellAt(int row, int column, EventObject editEvent) {
        multyChange = editEvent instanceof KeyEvent && ((KeyEvent) editEvent).getKeyCode() == KeyEvent.VK_F12;
        if (super.editCellAt(row, column, editEvent)) {
            //нужно для редактирования нефокусных ячеек
            changeSelectionInternal(row, column, false, false);
            return true;
        }
        return false;
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public boolean addColumn(final ClientPropertyDraw property) {
        if (columnProperties.indexOf(property) == -1) {
            List<ClientPropertyDraw> cells = logicsSupplier.getProperties();

            // конечно кривовато определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ind = cells.indexOf(property), ins = 0;

            Iterator<ClientPropertyDraw> icp = columnProperties.iterator();
            while (icp.hasNext() && cells.indexOf(icp.next()) < ind) {
                ins++;
            }

            columnProperties.add(ins, property);
            return true;
        } else
            return false;
    }

    public boolean removeColumn(ClientPropertyDraw property) {
        if (columnProperties.remove(property)) {
            values.remove(property);
            return true;
        }

        return false;
    }

    public void setColumnValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> pvalues) {
        values.put(property, pvalues);
    }

    public Object getSelectedValue(ClientPropertyDraw property) {
        return getSelectedValue(model.getMinPropertyIndex(property));
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
        int col = model.getMinPropertyIndex(property);
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

    private final List<Integer> orders = new ArrayList<Integer>();
    private final List<Boolean> orderDirections = new ArrayList<Boolean>();

    public void changeGridOrder(int col, Order modiType) throws IOException {
        ClientGroupObjectValue columnKey = model.getColumnKey(col);
        ClientPropertyDraw property = model.getColumnProperty(col);
        if (columnKey == null) {
            form.changeOrder(property, modiType);
        } else {
            form.changeOrder(property, modiType, columnKey);
        }

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

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        changeGridOrder(model.getMinPropertyIndex(property), modiType);
    }

    private Boolean getSortDirection(int column) {
        int ordNum = orders.indexOf(column);
        return (ordNum != -1) ? orderDirections.get(ordNum) : null;
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new GridTableHeader(columnModel);
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
            int num = 0;
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
                //oldMoveAction.actionPerformed(e);
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
                    changeSelection(initRow, initColumn, false, false);
                    isInternalNavigating = false;
                    return;
                }
            } while ((oRow != row || oColumn != column) && !isCellFocusable(row, column));
            isInternalNavigating = false;
            changeSelection(row, column, false, false);
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

            String toolTip = (String) columnModel.getColumn(index).getHeaderValue();

            ClientPropertyDraw cellView = model.getColumnProperty(modelIndex);
            toolTip += " (sID: " + cellView.getSID() + ")";
            return toolTip;
        }
    }
}