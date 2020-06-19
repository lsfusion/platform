package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.base.view.grid.cell.Context;
import lsfusion.gwt.client.form.controller.GFormController;

import java.util.List;

/**
 * Based on lsfusion.gwt.client.base.view.grid.DefaultDataGridBuilder
 */
public abstract class GPropertyTableBuilder<T> extends AbstractDataGridBuilder<T> {
    private final String rowStyle;
    private final String cellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;

    private int cellHeight = 16;
    private boolean updateCellHeight;

    public GPropertyTableBuilder(DataGrid table) {
        super(table);

        // Cache styles for faster access.
        GridStyle style = table.getStyle();
        rowStyle = style.dataGridRow();
        cellStyle = style.dataGridCell();
        firstColumnStyle = " " + style.dataGridFirstCell();
        lastColumnStyle = " " + style.dataGridLastCell();
    }

    @Override
    public void update(TableSectionElement tbodyElement, List<T> values, int minRenderedRow, int renderedRowCount, boolean columnsChanged) {
        super.update(tbodyElement, values, minRenderedRow, renderedRowCount, columnsChanged);
        updateCellHeight = false;
    }

    public void setCellHeight(int cellHeight) {
        updateCellHeight = true;
        this.cellHeight = cellHeight;
    }

    @Override
    public void buildRowImpl(int rowIndex, T rowValue, TableRowElement tr) {

        tr.setClassName(rowStyle);

        // Build the columns.
        int columnCount = cellTable.getColumnCount();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            Column<T, ?> column = cellTable.getColumn(columnIndex);

            // Create the cell styles.
            StringBuilder tdClasses = new StringBuilder(cellStyle);
            if (columnIndex == 0) {
                tdClasses.append(firstColumnStyle);
            }
            // The first and last column could be the same column.
            if (columnIndex == columnCount - 1) {
                tdClasses.append(lastColumnStyle);
            }

            TableCellElement td = tr.insertCell(columnIndex);
            td.setClassName(tdClasses.toString());

            updateTD(rowIndex, rowValue, td, columnIndex, true);

            renderCell(td, new Context(rowIndex, columnIndex, rowValue), column, rowValue);
        }
    }

    @Override
    protected void updateRowImpl(int rowIndex, T rowValue, int[] columnsToRedraw, TableRowElement tr) {
        int columnCount = cellTable.getColumnCount();

        assert columnCount == tr.getCells().getLength();

        if (columnsToRedraw == null) {
            if (columnCount > 0) {
                //td.nextSibling is faster than cells[index]
                //http://jsperf.com/nextsibling-vs-childnodes
                TableCellElement td = tr.getFirstChild().cast();
                updateCellImpl(rowIndex, rowValue, td, 0);

                int columnIndex = 1;
                while (columnIndex < columnCount) {
                    td = td.getNextSibling().cast();
                    updateCellImpl(rowIndex, rowValue, td, columnIndex);
                    ++columnIndex;
                }
            }
        } else {
            NodeList<TableCellElement> cells = tr.getCells();
            for (int columnIndex : columnsToRedraw) {
                TableCellElement td = cells.getItem(columnIndex);
                updateCellImpl(rowIndex, rowValue, td, columnIndex);
            }
        }
    }

    private void updateCellImpl(int rowIndex, T rowValue, TableCellElement td, int columnIndex) {
        Column<T, ?> column = cellTable.getColumn(columnIndex);

        updateTD(rowIndex, rowValue, td, columnIndex, updateCellHeight);

        updateCell(td, new Context(rowIndex, columnIndex, rowValue), column, rowValue);
    }

    // need this for mixing color
    public static String BKCOLOR = "lsfusion-bkcolor";

    protected void updateTD(int rowIndex, T rowValue, TableCellElement td, int columnIndex, boolean updateCellHeight) {
        if (updateCellHeight) {
            renderTD(td, cellHeight);

            Element divElement = td.getFirstChildElement();
            if (divElement != null) {
                divElement.getStyle().setHeight(cellHeight, Style.Unit.PX);
            }
        }

        String backgroundColor = getBackground(rowValue, rowIndex, columnIndex);
        td.setPropertyString(BKCOLOR, backgroundColor);
        GFormController.setBackgroundColor(td, backgroundColor);

        String foregroundColor = getForeground(rowValue, rowIndex, columnIndex);
        GFormController.setForegroundColor(td, foregroundColor);
    }

    public static void renderTD(Element td, int height) {
        setRowHeight(td, height);
        // setting line height to height it's the easiest way to align text to the center vertically, however it works only for single lines (which is ok for row data)
        td.getStyle().setLineHeight(height, Style.Unit.PX);
    }

    public static void setRowHeight(Element td, int height) {
        td.getStyle().setHeight(height, Style.Unit.PX);
    }

    public abstract String getBackground(T rowValue, int row, int column);
    public abstract String getForeground(T rowValue, int row, int column);
}

