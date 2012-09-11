package platform.gwt.form2.client.form.ui;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridDataRecord {
    public final int rowIndex;
    public final GGroupObjectValue key;

    private HashMap<Object, Object> values;
    private HashMap<Object, String> backgrounds;
    private HashMap<Object, String> foregrounds;

    public GridDataRecord(int rowIndex, GGroupObjectValue key) {
        this.rowIndex = rowIndex;
        this.key = key;
    }

    public void setAttribute(Object key, Object value) {
        if (value != null) {
            createValues().put(key, value);
        }
    }

    public Object getAttribute(Object key) {
        return values == null ? null : values.get(key);
    }

    public void setValue(int column, Object value) {
        setAttribute(column, value);
    }

    public Object getValue(int column) {
        return getAttribute(column);
    }

    public void setBackground(int column, Object color) {
        if (color != null) {
            createBackgrounds().put(column, color.toString());
        } else {
            createBackgrounds().remove(column);
        }
    }

    public String getBackground(int column) {
        return backgrounds == null ? null : backgrounds.get(column);
    }

    public void setForeground(int column, Object color) {
        if (color != null) {
            createForerounds().put(column, color.toString());
        } else {
            createForerounds().remove(column);
        }
    }

    public String getForeground(int column) {
        return foregrounds == null ? null : foregrounds.get(column);
    }

    private HashMap<Object, Object> createValues() {
        if (values == null) {
            values = new HashMap<Object, Object>();
        }
        return values;
    }

    private HashMap<Object, String> createBackgrounds() {
        if (backgrounds == null) {
            backgrounds = new HashMap<Object, String>();
        }
        return backgrounds;
    }

    private HashMap<Object, String> createForerounds() {
        if (foregrounds == null) {
            foregrounds = new HashMap<Object, String>();
        }
        return foregrounds;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GridDataRecord && key.equals(((GridDataRecord) obj).key);
    }

    @Override
    public String toString() {
        return "GridDataRecord{" +
                "values=" + values +
                '}';
    }

    public static ArrayList<GridDataRecord> createRecords(ArrayList<GPropertyDraw> columnProperties,
                                                          ArrayList<GGroupObjectValue> rowKeys,
                                                          List<GGroupObjectValue> columnKeys,
                                                          Map<GPropertyDraw, Map<GGroupObjectValue, Object>> values,
                                                          Map<GGroupObjectValue, Object> rowBackgroundValues,
                                                          Map<GGroupObjectValue, Object> rowForegroundValues,
                                                          Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues,
                                                          Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues) {
        ArrayList<GridDataRecord> result = new ArrayList<GridDataRecord>();
        for (int i = 0; i < rowKeys.size(); i++) {
            GGroupObjectValue rowKey = rowKeys.get(i);
            Object rowBackground = rowBackgroundValues.get(rowKey);
            Object rowForeground = rowForegroundValues.get(rowKey);

            GridDataRecord record = new GridDataRecord(i, rowKey);
            for (int j = 0; j < columnKeys.size(); j++) {
                GPropertyDraw columnProperty = columnProperties.get(j);
                GGroupObjectValue columnKey = columnKeys.get(j);
                GGroupObjectValue fullKey = columnKey == null ? rowKey : new GGroupObjectValue(rowKey, columnKey);

                Object value = values.get(columnProperty).get(fullKey);
                Object background = rowBackground == null && cellBackgroundValues.containsKey(columnProperty)
                                    ? cellBackgroundValues.get(columnProperty).get(fullKey)
                                    : rowBackground;
                Object foreground = rowForeground == null && cellForegroundValues.containsKey(columnProperty)
                                    ? cellForegroundValues.get(columnProperty).get(fullKey)
                                    : rowBackground;

                record.setValue(j, value);
                record.setBackground(j, background);
                record.setForeground(j, foreground);
            }
            result.add(record);
        }
        return result;
    }
}
