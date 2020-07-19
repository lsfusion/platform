package lsfusion.gwt.client.base.view.grid.cell;

import lsfusion.gwt.client.base.view.grid.Column;

/**
 * Contains information about the context of the Cell.
 */
public class Context {

    private final int columnIndex;
    private final int rowIndex;

    private final Column column;
    private final Object row;

    public Context(int rowIndex, int columnIndex, Column column, Object row) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.column = column;
        this.row = row;
        assert column != null && row != null;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public Column getColumn() {
        return column;
    }

    public Object getRow() {
        return row;
    }
}
