package platform.gwt.form.client.form.ui;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import platform.gwt.cellview.client.AbstractDataGridBuilder;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.Range;
import platform.gwt.cellview.client.cell.Cell;

import java.util.List;

import static platform.gwt.base.shared.GwtSharedUtils.nullEquals;

/**
 * Based on platform.gwt.cellview.client.DefaultDataGridBuilder
 */
public abstract class GPropertyTableBuilder<T> extends AbstractDataGridBuilder<T> {
    private final String evenRowStyle;
    private final String oddRowStyle;
    private final String cellStyle;
    private final String evenCellStyle;
    private final String oddCellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;

    private Double cellHeight;
    private boolean updateCellHeight;

    public GPropertyTableBuilder(DataGrid table) {
        super(table);

        // Cache styles for faster access.
        DataGrid.Style style = table.getResources().style();
        evenRowStyle = style.dataGridEvenRow();
        oddRowStyle = style.dataGridOddRow();
        cellStyle = style.dataGridCell();
        evenCellStyle = " " + style.dataGridEvenRowCell();
        oddCellStyle = " " + style.dataGridOddRowCell();
        firstColumnStyle = " " + style.dataGridFirstColumn();
        lastColumnStyle = " " + style.dataGridLastColumn();
    }

    @Override
    public void update(TableSectionElement tbodyElement, List<T> values, List<Range> updateRanges, int[] columnsToRedraw, boolean columnsChanged) {
        Double newCellHeight = getCellPixelHeight();
        if (!nullEquals(newCellHeight, cellHeight)) {
            updateCellHeight = true;
            cellHeight = newCellHeight;
        } else {
            updateCellHeight = false;
        }

        super.update(tbodyElement, values, updateRanges, columnsToRedraw, columnsChanged);
    }

    @Override
    public void buildRowImpl(int rowIndex, T rowValue, TableRowElement tr) {

        boolean isEven = rowIndex % 2 == 0;
        String trClasses = isEven ? evenRowStyle : oddRowStyle;

        tr.setClassName(trClasses);

        // Build the columns.
        int columnCount = cellTable.getColumnCount();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            Column<T, ?> column = cellTable.getColumn(columnIndex);

            // Create the cell styles.
            StringBuilder tdClasses = new StringBuilder(cellStyle);
            tdClasses.append(isEven ? evenCellStyle : oddCellStyle);
            if (columnIndex == 0) {
                tdClasses.append(firstColumnStyle);
            }
            // The first and last column could be the same column.
            if (columnIndex == columnCount - 1) {
                tdClasses.append(lastColumnStyle);
            }

            // Build the cell.
            HasHorizontalAlignment.HorizontalAlignmentConstant hAlign = column.getHorizontalAlignment();
            HasVerticalAlignment.VerticalAlignmentConstant vAlign = column.getVerticalAlignment();

            TableCellElement td = tr.insertCell(columnIndex);
            td.setClassName(tdClasses.toString());
            if (hAlign != null) {
                td.setAlign(hAlign.getTextAlignString());
            }
            if (vAlign != null) {
                td.setVAlign(vAlign.getVerticalAlignString());
            }

            updateTD(rowIndex, rowValue, td, columnIndex, true);

            // Add the inner div.
            DivElement div = Document.get().createDivElement();

            // Render the cell into the div.
            renderCell(div, new Cell.Context(rowIndex, columnIndex, rowValue), column, rowValue);

            td.appendChild(div);
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

        DivElement div = td.getFirstChild().cast();

        // Render the cell into the div.
        updateCell(div, new Cell.Context(rowIndex, columnIndex, rowValue), column, rowValue);
    }

    private void updateTD(int rowIndex, T rowValue, TableCellElement td, int columnIndex, boolean updateCellHeight) {
        if (updateCellHeight && cellHeight != null) {
            td.getStyle().setHeight(cellHeight, Style.Unit.PX);
            td.getStyle().setLineHeight(cellHeight, Style.Unit.PX);
        }

        String backgroundColor = getBackground(rowValue, rowIndex, columnIndex);
        if (backgroundColor != null) {
            td.getStyle().setBackgroundColor(backgroundColor);
        }

        String foregroundColor = getForeground(rowValue, rowIndex, columnIndex);
        if (foregroundColor != null) {
            td.getStyle().setColor(foregroundColor);
        }
    }

    public abstract String getBackground(T rowValue, int row, int column);
    public abstract String getForeground(T rowValue, int row, int column);
    public abstract Double getCellPixelHeight();
}

