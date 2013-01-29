/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package platform.gwt.cellview.client;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import platform.gwt.cellview.client.DataGrid.Style;
import platform.gwt.cellview.client.cell.Cell;

/**
 * Default cell table builder that renders row values into a grid of columns.
 *
 * @param <T> the data type of the rows.
 */
public class DefaultDataGridBuilder<T> extends AbstractDataGridBuilder<T> {

    private final String rowStyle;
    private final String cellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;

    public DefaultDataGridBuilder(DataGrid<T> cellTable) {
        super(cellTable);

        // Cache styles for faster access.
        Style style = cellTable.getResources().style();
        rowStyle = style.dataGridRow();
        cellStyle = style.dataGridCell();
        firstColumnStyle = " " + style.dataGridFirstColumn();
        lastColumnStyle = " " + style.dataGridLastColumn();
    }

    @Override
    public void buildRowImpl(int rowIndex, T rowValue, TableRowElement tr) {

        // Calculate the row styles.
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

        NodeList<TableCellElement> cells = tr.getCells();

        assert columnCount == cells.getLength();

        if (columnsToRedraw == null) {
            for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
                updateCellImpl(rowIndex, rowValue, cells, columnIndex);
            }
        } else {
            for (int columnIndex : columnsToRedraw) {
                updateCellImpl(rowIndex, rowValue, cells, columnIndex);
            }
        }
    }

    private void updateCellImpl(int rowIndex, T rowValue, NodeList<TableCellElement> cells, int columnIndex) {
        Column<T, ?> column = cellTable.getColumn(columnIndex);

        DivElement div = cells.getItem(columnIndex).getFirstChild().cast();

        // Render the cell into the div.
        updateCell(div, new Cell.Context(rowIndex, columnIndex, rowValue), column, rowValue);
    }
}
