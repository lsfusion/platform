package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.nullToString;

public class GridTableModel extends AbstractTableModel {
    private Object[][] data = new Object[0][0];
    private ClientPropertyDraw[] columnProps = new ClientPropertyDraw[0];
    private ClientGroupObjectValue[] columnKeys = new ClientGroupObjectValue[0];
    private String[] columnNames = new String[0];
    private Object[] rowHighlights = new Object[0];

    public void update(List<ClientPropertyDraw> columnProperties,
                             List<ClientGroupObjectValue> rowKeys,
                             Map<ClientPropertyDraw, List<ClientGroupObjectValue>> mapColumnKeys,
                             Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnCaptions,
                             Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values,
                             Map<ClientGroupObjectValue,Object> highlights) {

        List<ClientPropertyDraw> columnPropsList = new ArrayList<ClientPropertyDraw>();
        List<ClientGroupObjectValue> columnKeysList = new ArrayList<ClientGroupObjectValue>();

        for (ClientPropertyDraw property : columnProperties) {
            if (mapColumnKeys.containsKey(property)) {
                Map<ClientGroupObjectValue, Object> columnCaption = columnCaptions.get(property);
                for (ClientGroupObjectValue key : mapColumnKeys.get(property)) {
                    //не показываем колонку, если propertyCaption равно null
                    if (columnCaption==null || columnCaption.get(key) != null) {
                        columnKeysList.add(key);
                        columnPropsList.add(property);
                    }
                }
            } else {
                columnPropsList.add(property);
                columnKeysList.add(new ClientGroupObjectValue());
            }
        }

        if (columnProps.length != columnPropsList.size()) {
            columnProps = new ClientPropertyDraw[columnPropsList.size()];
            columnKeys = new ClientGroupObjectValue[columnKeysList.size()];
            columnNames = new String[columnKeysList.size()];
        }
        columnProps = columnPropsList.toArray(columnProps);
        columnKeys = columnKeysList.toArray(columnKeys);

        if (data.length==0 || data[0].length != columnProps.length || data.length != rowKeys.size()) {
            data = new Object[rowKeys.size()][columnProps.length];
        }

        if(rowHighlights.length != rowKeys.size())
            rowHighlights = new Object[rowKeys.size()];
        
        for (int i = 0; i < rowKeys.size(); ++i) {
            ClientGroupObjectValue rowKey = rowKeys.get(i);

            for (int j = 0; j < columnProps.length; ++j) {
                Map<ClientGroupObjectValue, Object> propValues = values.get(columnProps[j]);
                if (propValues != null) {
                    ClientGroupObjectValue columnKey = columnKeys[j];
                    ClientGroupObjectValue key = new ClientGroupObjectValue(rowKey, columnKey);
                    data[i][j] = propValues.get(key);
                } else {
                    data[i][j] = null;
                }
            }

            rowHighlights[i] = highlights.get(rowKey);
        }

        //заполняем имена колонок
        for (int i = 0; i < columnNames.length; ++i) {
            String resultCaption = nullToString(columnProps[i].getFullCaption());

            Map<ClientGroupObjectValue, Object> propColumnCaptions = columnCaptions.get(columnProps[i]);
            if (propColumnCaptions != null) {
                String columnCaption = nullToString(propColumnCaptions.get(columnKeys[i]));
                resultCaption += resultCaption.isEmpty()
                                 ? columnCaption
                                 : " [" + columnCaption + "]";
            }

            columnNames[i] = resultCaption;
        }
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return columnProps.length;
    }

    public boolean isCellEditable(int row, int column) {
        return !isCellReadOnly(row, column);
    }

    public boolean isCellFocusable(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return false;
        }

        Boolean focusable = columnProps[col].focusable;
        return focusable == null || focusable;
    }

    private boolean isCellReadOnly(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return false;
        }

        return columnProps[col].readOnly;
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void setValueAt(Object value, int row, int col) {
        if (columnProps[col].checkEquals && BaseUtils.nullEquals(value, data[row][col])) return;

        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

    public int getMinPropertyIndex(ClientPropertyDraw property) {
        for (int i = 0; i < columnProps.length; ++i) {
            if (property == columnProps[i]) {
                return i;
            }
        }
        return -1;
    }

    public ClientPropertyDraw getColumnProperty(int index) {
        return columnProps[index];
    }

    public ClientGroupObjectValue getColumnKey(int index) {
        return columnKeys[index];
    }

    public Object getHighlightValue(int index) {
        return rowHighlights[index];
    }
}
