package platform.gwt.form.client.form.ui;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;

public class GridDataRecord {
    public final int rowIndex;

    private GGroupObjectValue key;
    private String rowBackground;
    private String rowForeground;

    private HashMap<Object, Object> values;
    private HashMap<Object, String> backgrounds;
    private HashMap<Object, String> foregrounds;

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
        } else {
            createBackgrounds().remove(column);
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
        } else {
            createForegrounds().remove(column);
        }
    }

    public String getForeground(int column) {
        return rowForeground != null
               ? rowForeground
               : foregrounds == null ? null : foregrounds.get(column);
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

    private HashMap<Object, String> createForegrounds() {
        if (foregrounds == null) {
            foregrounds = new HashMap<Object, String>();
        }
        return foregrounds;
    }

    public void reinit(GGroupObjectValue newKey, Object newRowBackground, Object newRowForeground) {
        key = newKey;
        setRowBackground(newRowBackground);
        setRowForeground(newRowForeground);

        if (values != null) {
            values.clear();
        }
        if (backgrounds != null) {
            backgrounds.clear();
        }
        if (foregrounds != null) {
            foregrounds.clear();
        }
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
