package lsfusion.gwt.client.base.view.grid.cell;

import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.RowIndexHolder;

/**
 * Contains information about the context of the Cell.
 */
public class Cell {

    private final int columnIndex;
//    private final int rowIndex; // we don't want to store the index, since it can be incrementally changed (see inc* methods in AbstractDataGridBuilder)

    private final Column column;
    private final RowIndexHolder row;

    public Cell(int rowIndex, int columnIndex, Column column, RowIndexHolder row) {
//        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.column = column;
        this.row = row;
        // row - null and rownIndex are needed for the "column" row, which is not used for now
        assert (row == null && rowIndex == -1) || row != null && row.getRowIndex() == rowIndex;
        assert column != null;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getRowIndex() {
        return row.getRowIndex();
    }

    public Column getColumn() {
        return column;
    }

    public Object getRow() {
        return row;
    }
}
