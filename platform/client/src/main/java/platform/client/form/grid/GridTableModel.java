package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientObjectType;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.toCaption;

public class GridTableModel extends AbstractTableModel {
    private Object[][] data = new Object[0][0];
    private ClientPropertyDraw[] columnProps = new ClientPropertyDraw[0];
    private ClientGroupObjectValue[] columnKeys = new ClientGroupObjectValue[0];
    private String[] columnNames = new String[0];
    private Object[][] highlightData = new Object[0][];
    private Color[][] highlightColor = new Color[0][];

    public void update(ClientGroupObject groupObject,
                       List<ClientPropertyDraw> columnProperties,
                       List<ClientGroupObjectValue> rowKeys,
                       Map<ClientPropertyDraw, List<ClientGroupObjectValue>> mapColumnKeys,
                       Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnCaptions,
                       Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values,
                       Map<ClientGroupObjectValue, Object> mapRowHighlights,
                       Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mapHighlightValues) {

        List<ClientPropertyDraw> columnPropsList = new ArrayList<ClientPropertyDraw>();
        List<ClientGroupObjectValue> columnKeysList = new ArrayList<ClientGroupObjectValue>();

        for (ClientPropertyDraw property : columnProperties) {
            if (mapColumnKeys.containsKey(property)) {
                Map<ClientGroupObjectValue, Object> columnCaption = columnCaptions.get(property);
                for (ClientGroupObjectValue key : mapColumnKeys.get(property)) {
                    //не показываем колонку, если propertyCaption равно null
                    if ((columnCaption == null || columnCaption.get(key) != null) && !property.hide) {
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

        if (data.length == 0 || data[0].length != columnProps.length || data.length != rowKeys.size()) {
            data = new Object[rowKeys.size()][columnProps.length];
            highlightData = new Object[rowKeys.size()][columnProps.length];
            highlightColor = new Color[rowKeys.size()][columnProps.length];
        }

        for (int i = 0; i < rowKeys.size(); ++i) {
            ClientGroupObjectValue rowKey = rowKeys.get(i);
            Object rowHighlight = mapRowHighlights.get(rowKey);

            for (int j = 0; j < columnProps.length; ++j) {
                ClientGroupObjectValue cellKey = new ClientGroupObjectValue(rowKey, columnKeys[j]);

                Map<ClientGroupObjectValue, Object> propValues = values.get(columnProps[j]);
                Map<ClientGroupObjectValue, Object> highlightValues = mapHighlightValues.get(columnProps[j]);

                data[i][j] = propValues != null ? propValues.get(cellKey) : null;

                if (rowHighlight != null) {
                    highlightData[i][j] = rowHighlight;
                    highlightColor[i][j] = groupObject.highlightColor;
                } else if (highlightValues != null) {
                    highlightData[i][j] = highlightValues.get(cellKey);
                    highlightColor[i][j] = highlightData[i][j] != null ? columnProps[j].highlightColor : null;
                } else {
                    highlightData[i][j] = null;
                    highlightColor[i][j] = null;
                }
            }
        }

        //заполняем имена колонок
        for (int i = 0; i < columnNames.length; ++i) {
            String resultCaption = toCaption(columnProps[i].getFullCaption());

            Map<ClientGroupObjectValue, Object> propColumnCaptions = columnCaptions.get(columnProps[i]);
            if (propColumnCaptions != null) {
                String columnCaption = toCaption(propColumnCaptions.get(columnKeys[i]));
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

    public boolean isCellEditable(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return true;
        }

        return !columnProps[col].readOnly && !(columnProps[col].baseType instanceof ClientObjectType);
    }

    public boolean isCellFocusable(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return false;
        }

        Boolean focusable = columnProps[col].focusable;
        return focusable == null || focusable;
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void setValueAt(Object value, int row, int col) {
        throw new RuntimeException("things that should not be");
    }

    public void setValueAt(Object value, int row, int col, boolean multyChange) {
        if (!multyChange && columnProps[col].checkEquals && BaseUtils.nullEquals(value, data[row][col])) {
            return;
        }

        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

    public int getPropertyIndex(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        for (int i = 0; i < columnProps.length; ++i) {
            if (property == columnProps[i] && (columnKey==null || columnKey.equals(columnKeys[i]))) {
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

    public boolean isCellHighlighted(int row, int column) {
        return highlightData[row][column] != null;
    }

    public Color getHighlightColor(int row, int column) {
        return highlightColor[row][column];
    }
}
