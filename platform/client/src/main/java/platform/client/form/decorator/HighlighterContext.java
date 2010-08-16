package platform.client.form.decorator;

import platform.client.form.cell.CellTableInterface;

public class HighlighterContext {
    private CellTableInterface table;
    private Object value;
    private boolean selected;
    private boolean hasFocus;
    private int row;
    private int column;

    public HighlighterContext(CellTableInterface table, Object value, boolean selected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.value = value;
        this.selected = selected;
        this.hasFocus = hasFocus;
        this.row = row;
        this.column = column;
    }

    public CellTableInterface getTable() {
        return table;
    }

    public Object getValue() {
        return value;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isHasFocus() {
        return hasFocus;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
