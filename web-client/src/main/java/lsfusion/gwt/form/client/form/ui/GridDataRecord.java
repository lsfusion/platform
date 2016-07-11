package lsfusion.gwt.form.client.form.ui;

import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;

public class GridDataRecord {
    public final int rowIndex;

    private GGroupObjectValue key;
    private String rowBackground;
    private String rowForeground;

    private HashMap<Object, Object> values;
    private HashMap<Integer, Boolean> readOnlys;
    private HashMap<Integer, String> backgrounds;
    private HashMap<Integer, String> foregrounds;

    public GridDataRecord(GGroupObjectValue key) {
        this(-1, key);
    }

    public GridDataRecord(int rowIndex, GGroupObjectValue key) {
        this.rowIndex = rowIndex;
        this.key = key;
    }

    public void setAttribute(Object key, Object value) {
        if (value != null) {
            createValues().put(key, value);
        } else if (values != null) {
            values.remove(key);
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
        } else if (backgrounds != null) {
            backgrounds.remove(column);
        }
    }

    public String getBackground(int column) {
        return rowBackground != null
               ? rowBackground
               : backgrounds == null ? null : backgrounds.get(column);
    }

    public void setForeground(int column, Object color) {
        if (color != null) {
            createForegrounds().put(column, color.toString());
        } else if (foregrounds != null) {
            foregrounds.remove(column);
        }
    }

    public String getForeground(int column) {
        return rowForeground != null
               ? rowForeground
               : foregrounds == null ? null : foregrounds.get(column);
    }

    public void setReadOnly(int column, Object readOnly) {
        if (readOnly != null) {
            createReadOnlys().put(column, Boolean.TRUE);
        } else if (readOnlys != null) {
            readOnlys.remove(column);
        }
    }

    public boolean isReadonly(int column) {
        return readOnlys != null && readOnlys.get(column) != null;
    }

    public void setRowBackground(Object newRowBackground) {
        rowBackground = newRowBackground == null ? null : newRowBackground.toString();
    }

    public void setRowForeground(Object newRowForeground) {
        rowForeground = newRowForeground == null ? null : newRowForeground.toString();
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    private HashMap<Object, Object> createValues() {
        if (values == null) {
            values = new HashMap<>();
        }
        return values;
    }

    private HashMap<Integer, String> createBackgrounds() {
        if (backgrounds == null) {
            backgrounds = new HashMap<>();
        }
        return backgrounds;
    }

    private HashMap<Integer, String> createForegrounds() {
        if (foregrounds == null) {
            foregrounds = new HashMap<>();
        }
        return foregrounds;
    }

    private HashMap<Integer, Boolean> createReadOnlys() {
        if (readOnlys == null) {
            readOnlys = new HashMap<>();
        }
        return readOnlys;
    }

    public void reinit(GGroupObjectValue newKey, Object newRowBackground, Object newRowForeground) {
        key = newKey;
        setRowBackground(newRowBackground);
        setRowForeground(newRowForeground);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GridDataRecord && key.equals(((GridDataRecord) obj).key);
    }

    @Override
    public String toString() {
        return "GridDataRecord{" +
                "key=" + key +
                ", values=" + values +
                '}';
    }
}
