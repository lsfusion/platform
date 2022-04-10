package lsfusion.gwt.client.form.object.table.view;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashMap;
import java.util.Optional;

public class GridDataRecord {
    public final int rowIndex;

    private GGroupObjectValue key;
    private String rowBackground;
    private String rowForeground;

    private NativeStringMap<Object> values;
    private NativeStringMap<Boolean> readOnlys;
    private NativeStringMap<String> backgrounds;
    private NativeStringMap<String> foregrounds;

    public GridDataRecord(GGroupObjectValue key) {
        this(-1, key);
    }

    public GridDataRecord(int rowIndex, GGroupObjectValue key) {
        this.rowIndex = rowIndex;
        this.key = key;
    }

    public void setAttribute(String key, Object value) {
        if (value != null) {
            createValues().put(key, value);
        } else if (values != null) {
            values.remove(key);
        }
    }

    public Object getAttribute(String key) {
        return values == null ? null : values.get(key);
    }

    public void setValue(String column, Object value) {
        setAttribute(column, value);
    }

    public Object getValue(String column) {
        return getAttribute(column);
    }

    public void setLoading(String column, boolean loading) {
        setAttribute(column + "_loading", loading ? true : null);
    }

    public boolean isLoading(String column) {
        return getAttribute(column + "_loading") != null;
    }

    public Object getImage(String column) {
        return getAttribute(column + "_image");
    }

    public void setImage(String column, Object image) {
        setAttribute(column + "_image", image);
    }

    public void setBackground(String column, Object color) {
        if (color != null) {
            createBackgrounds().put(column, color.toString());
        } else if (backgrounds != null) {
            backgrounds.remove(column);
        }
    }

    public String getBackground(String column) {
        String background = backgrounds != null ? backgrounds.get(column) : null;
        return background != null ? background : rowBackground;
    }

    public void setForeground(String column, Object color) {
        if (color != null) {
            createForegrounds().put(column, color.toString());
        } else if (foregrounds != null) {
            foregrounds.remove(column);
        }
    }

    public String getForeground(String column) {
        String foreground = foregrounds != null ? foregrounds.get(column) : null;
        return foreground != null ? foreground : rowForeground;
    }

    public void setReadOnly(String column, Object readOnly) {
        if (readOnly != null) {
            createReadOnlys().put(column, Boolean.TRUE);
        } else if (readOnlys != null) {
            readOnlys.remove(column);
        }
    }

    public boolean isReadonly(String column) {
        return readOnlys != null && readOnlys.get(column) != null;
    }

    public void setRowBackground(Object newRowBackground) {
        rowBackground = newRowBackground == null ? null : newRowBackground.toString();
    }

    public String getRowBackground() {
        return rowBackground;
    }

    public void setRowForeground(Object newRowForeground) {
        rowForeground = newRowForeground == null ? null : newRowForeground.toString();
    }

    public String getRowForeground() {
        return rowForeground;
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    private NativeStringMap<Object> createValues() {
        if (values == null) {
            values = new NativeStringMap<>();
        }
        return values;
    }

    private NativeStringMap<String> createBackgrounds() {
        if (backgrounds == null) {
            backgrounds = new NativeStringMap<>();
        }
        return backgrounds;
    }

    private NativeStringMap<String> createForegrounds() {
        if (foregrounds == null) {
            foregrounds = new NativeStringMap<>();
        }
        return foregrounds;
    }

    private NativeStringMap<Boolean> createReadOnlys() {
        if (readOnlys == null) {
            readOnlys = new NativeStringMap<>();
        }
        return readOnlys;
    }

    public void reinit(GGroupObjectValue newKey, Object newRowBackground, Object newRowForeground) {
        key = newKey;
        setRowBackground(newRowBackground);
        setRowForeground(newRowForeground);
    }
}
