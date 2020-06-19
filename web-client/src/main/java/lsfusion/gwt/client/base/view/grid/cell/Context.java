package lsfusion.gwt.client.base.view.grid.cell;

/**
 * Contains information about the context of the Cell.
 */
public class Context {

    private final int column;
    private final int index;
    private final Object rowValue;

    /**
     * Create a new {@link Context}.
     *
     * @param index    the absolute index of the value
     * @param column   the column index of the cell, or 0
     * @param rowValue the unique key that represents the row value
     */
    public Context(int index, int column, Object rowValue) {
        this.index = index;
        this.column = column;
        this.rowValue = rowValue;
    }

    /**
     * Get the column index of the cell. If the view only contains a single
     * column, this method returns 0.
     *
     * @return the column index of the cell
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get the absolute index of the value.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the key that uniquely identifies the row object.
     *
     * @return the unique key
     */
    public Object getRowValue() {
        return rowValue;
    }
}
