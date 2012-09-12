package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.view.client.SelectionModel;

/**
 * Based on com.google.gwt.user.cellview.client.DefaultCellTableBuilder
 */
public abstract class GPropertyTableCellBuilder<T> extends AbstractCellTableBuilder<T> {
    private final String evenRowStyle;
    private final String oddRowStyle;
    private final String selectedRowStyle;
    private final String cellStyle;
    private final String evenCellStyle;
    private final String oddCellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;
    private final String selectedCellStyle;

    public GPropertyTableCellBuilder(GPropertyTable table) {
        super(table);

        // Cache styles for faster access.
        AbstractCellTable.Style style = table.getResources().style();
        evenRowStyle = style.evenRow();
        oddRowStyle = style.oddRow();
        selectedRowStyle = " " + style.selectedRow();
        cellStyle = style.cell();
        evenCellStyle = " " + style.evenRowCell();
        oddCellStyle = " " + style.oddRowCell();
        firstColumnStyle = " " + style.firstColumn();
        lastColumnStyle = " " + style.lastColumn();
        selectedCellStyle = " " + style.selectedRowCell();
    }

    @Override
    public void buildRowImpl(T rowValue, int rowIndex) {

        // Calculate the row styles.
        SelectionModel selectionModel = cellTable.getSelectionModel();
        boolean isSelected = selectionModel != null && rowValue != null && selectionModel.isSelected(rowValue);

        boolean isEven = rowIndex % 2 == 0;
        StringBuilder trClasses = new StringBuilder(isEven ? evenRowStyle : oddRowStyle);

        if (isSelected) {
            trClasses.append(selectedRowStyle);
        }

        // Add custom row styles.
        RowStyles<T> rowStyles = cellTable.getRowStyles();
        if (rowStyles != null) {
            String extraRowStyles = rowStyles.getStyleNames(rowValue, rowIndex);
            if (extraRowStyles != null) {
                trClasses.append(" ").append(extraRowStyles);
            }
        }

        // Build the row.
        TableRowBuilder tr = startRow();
        tr.className(trClasses.toString());

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
            if (isSelected) {
                tdClasses.append(selectedCellStyle);
            }
            // The first and last column could be the same column.
            if (columnIndex == columnCount - 1) {
                tdClasses.append(lastColumnStyle);
            }

            // Add class names specific to the cell.
            Cell.Context context = new Cell.Context(rowIndex, columnIndex, cellTable.getValueKey(rowValue));
            String cellStyles = column.getCellStyleNames(context, rowValue);
            if (cellStyles != null) {
                tdClasses.append(" ").append(cellStyles);
            }

            // Build the cell.
            HasHorizontalAlignment.HorizontalAlignmentConstant hAlign = column.getHorizontalAlignment();
            HasVerticalAlignment.VerticalAlignmentConstant vAlign = column.getVerticalAlignment();
            TableCellBuilder td = tr.startTD();
            td.className(tdClasses.toString());
            if (hAlign != null) {
                td.align(hAlign.getTextAlignString());
            }
            if (vAlign != null) {
                td.vAlign(vAlign.getVerticalAlignString());
            }

            String backgroundColor = getBackground(rowValue, rowIndex, columnIndex);
            if (backgroundColor != null) {
                td.style().trustedBackgroundColor(backgroundColor);
            }

            String foregroundColor = getForeground(rowValue, rowIndex, columnIndex);
            if (foregroundColor != null) {
                td.style().trustedColor(foregroundColor);
            }

            // Add the inner div.
            DivBuilder div = td.startDiv();
            div.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();

            // Render the cell into the div.
            renderCell(div, context, column, rowValue);

            // End the cell.
            div.endDiv();
            td.endTD();
        }

        // End the row.
        tr.endTR();
    }

    public abstract String getBackground(T rowValue, int row, int column);
    public abstract String getForeground(T rowValue, int row, int column);
}

