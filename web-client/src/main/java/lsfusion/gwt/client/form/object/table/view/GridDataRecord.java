package lsfusion.gwt.client.form.object.table.view;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.grid.RowIndexHolder;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;

public class GridDataRecord implements RowIndexHolder {
    public int rowIndex;

    private GGroupObjectValue key;
    private String rowBackground;
    private String rowForeground;

    private NativeStringMap<Object> values;

    public GridDataRecord(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    @Override
    public int getRowIndex() {
        return rowIndex;
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

    public void setValue(String column, PValue value) {
        setAttribute(column, value);
    }

    public PValue getValue(String column) {
        return (PValue) getAttribute(column);
    }

    public void setLoading(String column, boolean loading) {
        setAttribute(column + "_loading", loading ? true : null);
    }

    public boolean isLoading(String column) {
        return getAttribute(column + "_loading") != null;
    }

    public AppBaseImage getImage(String column) {
        return (AppBaseImage) getAttribute(column + "_image");
    }

    public void setImage(String column, AppBaseImage image) {
        setAttribute(column + "_image", image);
    }

    public void setElementClass(String column, String elementClass) {
        setAttribute(column + "_elementClass", elementClass);
    }

    public String getElementClass(String column) {
        return (String) getAttribute(column + "_elementClass");
    }

    public void setValueElementClass(String column, String elementClass) {
        setAttribute(column + "_valueElementClass", elementClass);
    }

    public String getValueElementClass(String column) {
        return (String) getAttribute(column + "_valueElementClass");
    }

    public void setFont(String column, GFont font) {
        setAttribute(column + "_font", font);
    }

    public GFont getFont(String column) {
        return (GFont) getAttribute(column + "_font");
    }

    public void setBackground(String column, String color) {
        setAttribute(column + "_background", color);
    }

    public String getBackground(String column) {
        String background = (String) getAttribute(column + "_background");
        return background != null ? background : rowBackground;
    }

    public void setPlaceholder(String column, String placeholder) {
        setAttribute(column + "_placeholder", placeholder);
    }

    public String getPlaceholder(String column) {
        return (String) getAttribute(column + "_placeholder");
    }

    public void setPattern(String column, String pattern) {
        setAttribute(column + "_pattern", pattern);
    }

    public String getPattern(String column) {
        return (String) getAttribute(column + "_pattern");
    }

    public void setRegexp(String column, String regexp) {
        setAttribute(column + "_regexp", regexp);
    }

    public String getRegexp(String column) {
        return (String) getAttribute(column + "_regexp");
    }

    public void setRegexpMessage(String column, String regexpMessage) {
        setAttribute(column + "_regexpmessage", regexpMessage);
    }

    public String getRegexpMessage(String column) {
        return (String) getAttribute(column + "_regexpmessage");
    }

    public void setValueTooltip(String column, String valueTooltip) {
        setAttribute(column + "_valueTooltip", valueTooltip);
    }

    public String getValueTooltip(String column) {
        return (String) getAttribute(column + "_valueTooltip");
    }

    public void setForeground(String column, String color) {
        setAttribute(column + "_foreground", color);
    }

    public String getForeground(String column) {
        String foreground = (String) getAttribute(column + "_foreground");
        return foreground != null ? foreground : rowForeground;
    }

    public void setReadOnly(String column, Boolean readOnly) {
        setAttribute(column + "_readonly", readOnly);
    }

    public Boolean isReadonly(String column) {
        return (Boolean) getAttribute(column + "_readonly");
    }

    public void setRowBackground(PValue newRowBackground) {
        rowBackground = PValue.getColorStringValue(newRowBackground);
    }

    public String getRowBackground() {
        return rowBackground;
    }

    public void setRowForeground(PValue newRowForeground) {
        rowForeground = PValue.getColorStringValue(newRowForeground);
    }

    public String getRowForeground() {
        return rowForeground;
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    public static final int objectExpandingIndex = -1;
    public int getExpandingIndex() {
        return objectExpandingIndex;
    }

    private NativeStringMap<Object> createValues() {
        if (values == null) {
            values = new NativeStringMap<>();
        }
        return values;
    }

    public void setKey(GGroupObjectValue newKey) {
        key = newKey;
    }
}
