package platform.client.form.grid;

import platform.client.form.ClientFormTable;
import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.sort.GridHeaderRenderer;
import platform.client.form.sort.GridHeaderMouseListener;
import platform.client.form.cell.ClientCellViewTable;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.logics.*;
import platform.client.SwingUtils;
import platform.interop.Scroll;
import platform.interop.Order;
import platform.base.BaseUtils;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.*;
import java.util.List;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;

public abstract class GridTable extends ClientFormTable
                               implements ClientCellViewTable {

    private final List<ClientCellView> gridColumns = new ArrayList<ClientCellView>();

    private List<ClientGroupObjectValue> gridRows = new ArrayList<ClientGroupObjectValue>();
    // приходится давать доступ к gridRows, так как контроллеру нужно заполнять значения колонок на основе ключей рядов
    public List<ClientGroupObjectValue> getGridRows() {
        return gridRows;
    }

    private ClientGroupObjectValue currentObject;

    private final Map<ClientCellView, Map<ClientGroupObjectValue,Object>> gridValues = new HashMap<ClientCellView, Map<ClientGroupObjectValue,Object>>();

    private final Model model;
    private final JTableHeader header;

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

        createDefaultColumnsFromModel();
        for (ClientCellView property : gridColumns) {

            TableColumn column = getColumnModel().getColumn(gridColumns.indexOf(property));
            column.setMinWidth(property.getMinimumWidth(this));
            column.setPreferredWidth(property.getPreferredWidth(this));
            column.setMaxWidth(property.getMaximumWidth(this));
        }

        if (gridColumns.size() != 0) {
            needToBeShown();
        } else {
            needToBeHidden();
        }

    }
    
    protected abstract void needToBeShown();
    protected abstract void needToBeHidden();

    private boolean fitWidth() {

        int minWidth = 0;
        int columnCount = getColumnCount();
        TableColumnModel columnModel = getColumnModel();

        for (int i = 0; i < columnCount; i++)
            minWidth += columnModel.getColumn(i).getMinWidth();

//                    System.out.println(this + " ~ " + groupObject.toString() + " : " + minWidth + " - " + pane.getWidth());

        // тут надо смотреть pane, а не саму table
        return (minWidth < getParent().getWidth());
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return fitWidth();
    }

    private int pageSize = 50;

    @Override
    public void doLayout() {

//                    System.out.println(this + " ~ " + groupObject.toString() + " : " + minWidth + " - " + pane.getWidth());

        if (fitWidth()) {
            autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
        } else {
            autoResizeMode = JTable.AUTO_RESIZE_OFF;
        }
        super.doLayout();
    }

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

        //noinspection SimplifiableIfStatement
        if (form.isReadOnly() && ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))) return false;

        return super.processKeyBinding(ks, ae, condition, pressed);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private final GroupObjectLogicsSupplier logicsSupplier;

    // пока пусть GridTable напрямую общается с формой, а не через Controller, так как ей много о чем надо с ней говорить, а Controller будет просто бюрократию создавать
    private final ClientForm form;
    public ClientForm getForm() {
        return form;
    }

    private ClientCellView currentCell;
    public ClientCellView getCurrentCell() { return currentCell; }

    public GridTable(GroupObjectLogicsSupplier ilogicsSupplier, ClientForm iform) {

        logicsSupplier = ilogicsSupplier;
        form = iform;

        model = new Model();
        setModel(model);

        header = getTableHeader();

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
//                            System.out.println("changeSel");
                final ClientGroupObjectValue changeObject = model.getSelectedObject();
                assert changeObject!=null;
                SwingUtils.invokeLaterSingleAction(logicsSupplier.getGroupObject().getActionID()
                        , new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            if(changeObject.equals(model.getSelectedObject())) {
                                currentObject = model.getSelectedObject(); // нужно менять текущий выбранный объект для правильного скроллирования
                                form.changeGroupObject(logicsSupplier.getGroupObject(), model.getSelectedObject());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении текущего объекта", e);
                        }
                    }
                }, 50);
            }
        });

        getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                currentCell = model.getSelectedCell();
            }
        });

        header.setDefaultRenderer(new GridHeaderRenderer(header.getDefaultRenderer()) {

            protected Boolean getSortDirection(int column) {
                return GridTable.this.getSortDirection(getCellView(column));
            }
        });

        header.addMouseListener(new GridHeaderMouseListener() {

            protected Boolean getSortDirection(int column) {
                return GridTable.this.getSortDirection(getCellView(column));
            }

            protected TableColumnModel getColumnModel() {
                return GridTable.this.getColumnModel();
            }

            protected void changeOrder(int column, Order modiType) {

                try {
                    changeGridOrder(getCellView(column), modiType);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка изменении сортировки", e);
                }

                header.repaint();
            }
        });

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ce) {

                // Listener срабатывает в самом начале, когда компонент еще не расположен
                // В таком случае нет смысла вызывать изменение pageSize
                if (getParent().getHeight() == 0)
                    return;

                int newPageSize = getParent().getHeight() / getRowHeight() + 1;
//                            System.out.println(groupObject.toString() + getParent().getViewport().getHeight() + " - " + getRowHeight() + " ; " + pageSize + " : " + newPageSize);
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

            @Override
            public void mouseClicked(MouseEvent e) {
                if (form.isReadOnly() && e.getClickCount() > 1) form.okPressed();
            }
        });

    }

    public boolean addColumn(ClientCellView property) {

        if (gridColumns.indexOf(property) == -1) {

            Iterator<ClientCellView> icp = gridColumns.iterator();

            List<ClientCellView> cells = logicsSupplier.getCells();

            // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ind = cells.indexOf(property), ins = 0;

            while (icp.hasNext() && cells.indexOf(icp.next()) < ind) { ins++; }

            gridColumns.add(ins, property);

            return true;
        } else
            return false;
    }

    public boolean removeColumn(ClientCellView property) {

        if (gridColumns.remove(property)) {

            gridValues.remove(property);
            return true;
        }

        return false;

    }

    public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {

        int oldindex = gridRows.indexOf(currentObject);

        gridRows = igridObjects;

        // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
        // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
        setSize(getWidth(), getRowHeight() * getRowCount());

        final int newindex = gridRows.indexOf(currentObject);

        //надо сдвинуть ViewPort - иначе дергаться будет

        if (newindex != -1) {

            if (oldindex != -1 && newindex != oldindex) {

                final Point ViewPos = ((JViewport)getParent()).getViewPosition();
                final int dltpos = (newindex-oldindex) * getRowHeight();
                ViewPos.y += dltpos;
                if (ViewPos.y < 0) ViewPos.y = 0;
                ((JViewport)getParent()).setViewPosition(ViewPos);
            }

            selectRow(newindex);
        }

    }

    public void selectObject(ClientGroupObjectValue value) {

        int oldindex = getSelectionModel().getLeadSelectionIndex();
        int newindex = gridRows.indexOf(value);
        if (newindex != -1 && newindex != oldindex) {
            //Выставляем именно первую активную колонку, иначе фокус на таблице - вообще нереально увидеть
            selectRow(newindex);
        }
    }

    public void setColumnValues(ClientCellView property, Map<ClientGroupObjectValue,Object> values) {

        gridValues.put(property, values);
        repaint();

    }

    public Object getSelectedValue(ClientCellView property) {
        return getSelectedValue(gridColumns.indexOf(property));
    }

    private Object getSelectedValue(int col) {

        int row = getSelectedRow();
        if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount())
            return getValueAt(row, col);
        else
            return null;
    }

    public boolean processKeyEvent(KeyStroke ks, KeyEvent e) {

        for (ClientCellView columns : gridColumns) {
            if (ks.equals(columns.editKey)) {
                int leadRow = getSelectionModel().getLeadSelectionIndex();
                if (leadRow != -1 && !isEditing()) {
                    if (editCellAt(leadRow, gridColumns.indexOf(columns)))
                        return true;
                }
                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Модель данных ------------------------------------ //
    // ---------------------------------------------------------------------------------------------- //

    class Model extends AbstractTableModel {

        public String getColumnName(int col) {
              return gridColumns.get(col).caption;
        }

        public int getRowCount() {
            return gridRows.size();
        }

        public int getColumnCount() {
            return gridColumns.size();
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public Object getValueAt(int row, int col) {

            return gridValues.get(gridColumns.get(col)).get(gridRows.get(row));
        }

        public void setValueAt(Object value, int row, int col) {

            // частный случай - не работает если меняется не само это свойство, а какое-то связанное
            if (BaseUtils.nullEquals(value, getValueAt(row, col))) return;

            try {
                form.changeProperty(gridColumns.get(col), value);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при изменении значения свойства", e);
            }
        }

        public ClientGroupObjectValue getSelectedObject() {
            int rowModel = convertRowIndexToModel(getSelectedRow());
            if (rowModel < 0 || rowModel >= getRowCount())
                return null;

            return gridRows.get(rowModel);
        }

        public ClientCellView getSelectedCell() {

            int colView = getSelectedColumn();
            if (colView < 0 || colView >= getColumnCount())
                return null;

            int colModel = convertColumnIndexToModel(colView);
            if (colModel < 0)
                return null;

            return gridColumns.get(colModel);
        }
    }

    public boolean isDataChanging() {
        return true;
    }

    public ClientCellView getCellView(int col) {
        return gridColumns.get(col);
    }

    private final List<ClientCellView> orders = new ArrayList<ClientCellView>();
    private final List<Boolean> orderDirections = new ArrayList<Boolean>();

    public void changeGridOrder(ClientCellView property, Order modiType) throws IOException {

        form.changeOrder(property, modiType);
        
        int ordNum;
        switch(modiType) {
            case REPLACE:
                orders.clear();
                orderDirections.clear();

                orders.add(property);
                orderDirections.add(true);
                break;
            case ADD:
                orders.add(property);
                orderDirections.add(true);
                break;
            case DIR:
                ordNum = orders.indexOf(property);
                orderDirections.set(ordNum, !orderDirections.get(ordNum));
                break;
            case REMOVE:
                ordNum = orders.indexOf(property);
                orders.remove(ordNum);
                orderDirections.remove(ordNum);
                break;
        }
    }

    private Boolean getSortDirection(ClientCellView property) {
        int ordNum = orders.indexOf(property);
        return (ordNum != -1) ? orderDirections.get(ordNum) : null;
    }

}