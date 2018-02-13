package lsfusion.client.form.grid;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientObjectType;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.toCaption;

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
                ClientPropertyDraw columnProp = columnProps[j];
                ClientGroupObjectValue cellKey = new ClientGroupObjectValue(rowKey, columnKeys[j]);

                Map<ClientGroupObjectValue, Object> propValues = values.get(columnProp);
                Map<ClientGroupObjectValue, Object> readOnlyValues = mapReadOnlyValues.get(columnProp);
                Map<ClientGroupObjectValue, Object> backgroundValues = mapBackgroundValues.get(columnProp);
                Map<ClientGroupObjectValue, Object> foregroundValues = mapForegroundValues.get(columnProp);

                data[i][j] = propValues == null ? null : columnProp.baseType.transformServerValue(propValues.get(cellKey));
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

    public void updateColumns(List<ClientPropertyDraw> columnProperties,
                              Map<ClientPropertyDraw, List<ClientGroupObjectValue>> mapColumnKeys,
                              Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnCaptions,
                              Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnShowIfs) {

        //разбиваем на группы свойств, которые будут идти чередуясь для каждого ключа из групп в колонках ("шахматка")
        Table<String, List<ClientGroupObject>, Integer> columnGroupsIndices = HashBasedTable.create();
        List<List<ClientPropertyDraw>> columnGroups = new ArrayList<>();
        List<List<ClientGroupObjectValue>> columnGroupsColumnKeys = new ArrayList<>();
        
        for (ClientPropertyDraw property : columnProperties) {
            if (property.columnsName != null && property.columnGroupObjects != null) {
                List<ClientPropertyDraw> columnGroup;

                Integer groupInd = columnGroupsIndices.get(property.columnsName, property.columnGroupObjects);
                if (groupInd != null) {
                    // уже было свойство с такими же именем и группами в колонках
                    columnGroup = columnGroups.get(groupInd);
                } else {
                    // новая группа свойств
                    columnGroup = new ArrayList<>();

                    List<ClientGroupObjectValue> propColumnKeys = mapColumnKeys.get(property);
                    if (propColumnKeys == null) {
                        propColumnKeys = ClientGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                    }
                    
                    columnGroupsColumnKeys.add(propColumnKeys);
                    columnGroupsIndices.put(property.columnsName, property.columnGroupObjects, columnGroups.size());
                    columnGroups.add(columnGroup);
                }
                columnGroup.add(property);
            } else {
                List<ClientGroupObjectValue> propColumnKeys = mapColumnKeys.get(property);
                if (propColumnKeys == null) {
                    propColumnKeys = ClientGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                }
                columnGroupsColumnKeys.add(propColumnKeys);
                columnGroups.add(Collections.singletonList(property));
            }
        }

        List<ClientPropertyDraw> columnPropsList = new ArrayList<>();
        List<ClientGroupObjectValue> columnKeysList = new ArrayList<>();
        for (int i = 0; i < columnGroups.size(); i++) {
            List<ClientPropertyDraw> columnGroup = columnGroups.get(i);
            List<ClientGroupObjectValue> columnKeys = columnGroupsColumnKeys.get(i);
            
            if (columnGroup.size() == 1) {
                for (ClientPropertyDraw property : columnGroup) {
                    //showIfs.get() вынесен из двойного цикла
                    Map<ClientGroupObjectValue, Object> columnShowIf = columnShowIfs.get(property);
                    for (ClientGroupObjectValue columnKey : columnKeys) {
                        if (columnShowIf == null || columnShowIf.get(columnKey) != null) {
                            columnPropsList.add(property);
                            columnKeysList.add(columnKey);
                        }
                    }
                }
            } else {
                for (ClientGroupObjectValue columnKey : columnKeys) {
                    for (ClientPropertyDraw property : columnGroup) {
                        Map<ClientGroupObjectValue, Object> columnShowIf = columnShowIfs.get(property);
                        if (columnShowIf == null || columnShowIf.get(columnKey) != null) {
                            columnPropsList.add(property);
                            columnKeysList.add(columnKey);
                        }
                    }
                }
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
                    columnProps[i].getDynamicCaption(propColumnCaptions.get(columnKeys[i])) :
                    toCaption(columnProps[i].getHTMLCaption());
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
}
