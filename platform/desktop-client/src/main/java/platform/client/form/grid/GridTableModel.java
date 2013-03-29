package platform.client.form.grid;

import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientObjectType;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

import static platform.base.BaseUtils.toCaption;

public class GridTableModel extends AbstractTableModel {
    private Object[][] data = new Object[0][0];
    private boolean[][] readOnly = new boolean[0][0];
    private ClientPropertyDraw[] columnProps = new ClientPropertyDraw[0];
    private ClientGroupObjectValue[] columnKeys = new ClientGroupObjectValue[0];
    private String[] columnNames = new String[0];
    private Color[][] backgroundColor = new Color[0][];
    private Color[][] foregroundColor = new Color[0][];

    public void updateRows(List<ClientGroupObjectValue> rowKeys,
                           Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values,
                           Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mapReadOnlyValues,
                           Map<ClientGroupObjectValue, Object> mapRowBackgroundValues,
                           Map<ClientGroupObjectValue, Object> mapRowForegroundValues,
                           Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mapBackgroundValues,
                           Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mapForegroundValues) {

        if (data.length == 0 || data[0].length != columnProps.length || data.length != rowKeys.size()) {
            data = new Object[rowKeys.size()][columnProps.length];
            readOnly = new boolean[rowKeys.size()][columnProps.length];
            backgroundColor = new Color[rowKeys.size()][columnProps.length];
            foregroundColor = new Color[rowKeys.size()][columnProps.length];
        }

        for (int i = 0; i < rowKeys.size(); ++i) {
            ClientGroupObjectValue rowKey = rowKeys.get(i);
            Object rowBackground = mapRowBackgroundValues.get(rowKey);
            Object rowForeground = mapRowForegroundValues.get(rowKey);

            for (int j = 0; j < columnProps.length; ++j) {
                ClientGroupObjectValue cellKey = new ClientGroupObjectValue(rowKey, columnKeys[j]);

                Map<ClientGroupObjectValue, Object> propValues = values.get(columnProps[j]);
                Map<ClientGroupObjectValue, Object> readOnlyValues = mapReadOnlyValues.get(columnProps[j]);
                Map<ClientGroupObjectValue, Object> backgroundValues = mapBackgroundValues.get(columnProps[j]);
                Map<ClientGroupObjectValue, Object> foregroundValues = mapForegroundValues.get(columnProps[j]);

                data[i][j] = propValues != null ? propValues.get(cellKey) : null;
                readOnly[i][j] = readOnlyValues != null && readOnlyValues.get(cellKey) != null;

                if (rowBackground != null) {
                    backgroundColor[i][j] = (Color) rowBackground;
                } else if (backgroundValues != null) {
                    backgroundColor[i][j] = (Color) backgroundValues.get(cellKey);
                } else {
                    backgroundColor[i][j] = null;
                }

                if (rowForeground != null) {
                    foregroundColor[i][j] = (Color) rowForeground;
                } else if (foregroundValues != null) {
                    foregroundColor[i][j] = (Color) foregroundValues.get(cellKey);
                } else {
                    foregroundColor[i][j] = null;
                }
            }
        }
    }

    public void updateColumns(List<ClientPropertyDraw> columnProperties, Map<ClientPropertyDraw, List<ClientGroupObjectValue>> mapColumnKeys, Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnCaptions) {
        List<ClientPropertyDraw> columnPropsList = new ArrayList<ClientPropertyDraw>();
        List<ClientGroupObjectValue> columnKeysList = new ArrayList<ClientGroupObjectValue>();

        Collections.sort(columnProperties, COMPARATOR);

        for (ClientPropertyDraw property : columnProperties) {
            if (mapColumnKeys.containsKey(property)) {
                Map<ClientGroupObjectValue, Object> columnCaption = columnCaptions.get(property);
                for (ClientGroupObjectValue key : mapColumnKeys.get(property)) {
                    //не показываем колонку, если propertyCaption равно null
                    Boolean needToHide = property.hideUser == null ? property.hide : property.hideUser;
                    if ((columnCaption == null || columnCaption.get(key) != null) && !needToHide) {
                        columnKeysList.add(key);
                        columnPropsList.add(property);
                    }
                }
            } else {
                columnPropsList.add(property);
                columnKeysList.add(ClientGroupObjectValue.EMPTY);
            }
        }

        if (columnProps.length != columnPropsList.size()) {
            columnProps = new ClientPropertyDraw[columnPropsList.size()];
            columnKeys = new ClientGroupObjectValue[columnKeysList.size()];
            columnNames = new String[columnKeysList.size()];
        }
        columnProps = columnPropsList.toArray(columnProps);
        columnKeys = columnKeysList.toArray(columnKeys);

        //заполняем имена колонок
        for (int i = 0; i < columnNames.length; ++i) {
            Map<ClientGroupObjectValue, Object> propColumnCaptions = columnCaptions.get(columnProps[i]);
            columnNames[i] = propColumnCaptions != null ?
                                       columnProps[i].getDynamicCaption(toCaption(propColumnCaptions.get(columnKeys[i]))) :
                                       toCaption(columnProps[i].getCaption());
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
        if (!isCellInBounds(row, col)) {
            return true;
        }

        return !readOnly[row][col] && !columnProps[col].isReadOnly() && !(columnProps[col].baseType instanceof ClientObjectType);
    }

    public boolean isCellFocusable(int row, int col) {
        if (!isCellInBounds(row, col)) {
            return false;
        }

        Boolean focusable = columnProps[col].focusable;
        return focusable == null || focusable;
    }

    private boolean isCellInBounds(int row, int col) {
        return row >= 0 && row < getRowCount() && col >= 0 && col < getColumnCount();
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
    }

    public int getPropertyIndex(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        for (int i = 0; i < columnProps.length; ++i) {
            if (property == columnProps[i] && (columnKey == null || columnKey.equals(columnKeys[i]))) {
                return i;
            }
        }
        return -1;
    }

    public ClientPropertyDraw getColumnProperty(int index) {
        return columnProps[index];
    }

    public ClientPropertyDraw[] getColumnProperties() {
        return columnProps.clone();
    }

    public ClientGroupObjectValue getColumnKey(int index) {
        return columnKeys[index];
    }

    public Color getBackgroundColor(int row, int column) {
        return backgroundColor[row][column];
    }

    public Color getForegroundColor(int row, int column) {
        return foregroundColor[row][column];
    }

    private static Comparator<ClientPropertyDraw> COMPARATOR = new Comparator<ClientPropertyDraw>() {
        public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
            if (c1.orderUser == null || c2.orderUser == null)
                return 0;
            else
                return c1.orderUser - c2.orderUser;
        }
    };

}
