package lsfusion.gwt.client.form.object.table.view;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.HashMap;

public class GridDataRecord {
    public final int rowIndex;

    private GGroupObjectValue key;
    private String rowBackground;
    private String rowForeground;

    private NativeHashMap<Object, Object> values;
    private NativeHashMap<Integer, Boolean> readOnlys;
    private NativeHashMap<Integer, String> backgrounds;
    private NativeHashMap<Integer, String> foregrounds;

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

    private NativeHashMap<Object, Object> createValues() {
        if (values == null) {
            values = new NativeHashMap<>();
        }
        return values;
    }

    private NativeHashMap<Integer, String> createBackgrounds() {
        if (backgrounds == null) {
            backgrounds = new NativeHashMap<>();
        }
        return backgrounds;
    }

    private NativeHashMap<Integer, String> createForegrounds() {
        if (foregrounds == null) {
            foregrounds = new NativeHashMap<>();
        }
        return foregrounds;
    }

    private NativeHashMap<Integer, Boolean> createReadOnlys() {
        if (readOnlys == null) {
            readOnlys = new NativeHashMap<>();
        }
        return readOnlys;
    }

    public void reinit(GGroupObjectValue newKey, Object newRowBackground, Object newRowForeground) {
        key = newKey;
        setRowBackground(newRowBackground);
        setRowForeground(newRowForeground);
    }
}
