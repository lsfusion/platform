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
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;

import java.util.List;
import java.util.function.BiPredicate;

import static lsfusion.gwt.client.base.GwtClientUtils.removeAllChildren;

/**
 * Builder used to construct a CellTable.
 *
 * @param <T> the row data type
 */
public abstract class AbstractDataGridBuilder<T> {

    /**
     * The attribute used to indicate that an element contains a cell.
     */
    public static final String COLUMN_ATTRIBUTE = "__gwt_column";
    public static final String COLUMN_CLASS = "class__gwt_column";

    /**
     * The attribute used to specify the logical row index.
     */
    private static final String ROW_ATTRIBUTE = "__gwt_row";

    protected final DataGrid<T> cellTable;

    /**
     * Construct a new table builder.
     *
     * @param cellTable the table this builder will build rows for
     */
    public AbstractDataGridBuilder(DataGrid<T> cellTable) {
        this.cellTable = cellTable;
    }

    public void update(TableSectionElement tbodyElement, List<T> values, boolean rerenderRows, int[] columnsToRedraw) {
        //assertion that updateRanges is sorted

        int newRowCount = values.size();
        int rowCount = tbodyElement.getChildCount();
        assert columnsToRedraw == null || newRowCount == rowCount;

        if (rowCount > newRowCount) {
            for (int i = rowCount - 1; i >= newRowCount; --i) {
                tbodyElement.deleteRow(i);
            }
            rowCount = newRowCount;
        }

        if (rerenderRows) {
            //rebuild rows if columns have been changed
            if (rowCount > 0) {
                TableRowElement tr = tbodyElement.getFirstChild().cast();
                rebuildRow(0, values.get(0), tr);
                for (int i = 1; i < rowCount; ++i) {
                    tr = tr.getNextSibling().cast();
                    rebuildRow(i, values.get(i), tr);
                }
            }
        } else {
            if (rowCount > 0) {
                //update changed rows
                NodeList<TableRowElement> rows = tbodyElement.getRows();
                TableRowElement tr = rows.getItem(0);
                updateRow(0, values.get(0), columnsToRedraw, tr);
                for (int i = 1; i < rowCount; ++i) {
                    tr = tr.getNextSibling().cast();
                    updateRow(i, values.get(i), columnsToRedraw, tr);
                }
            }
        }

        //build new rows
        for (int i = rowCount; i < newRowCount; ++i) {
            buildRow(tbodyElement, i, values.get(i));
        }
    }

    private void rebuildRow(int rowIndex, T rowValue, TableRowElement tr) {
        removeAllChildren(tr);
        setRowValueIndex(tr, rowIndex);
        buildRowImpl(rowIndex, rowValue, tr);
    }

    private void buildRow(TableSectionElement tbodyElement, int rowIndex, T rowValue) {
        TableRowElement tr = tbodyElement.insertRow(-1);
        setRowValueIndex(tr, rowIndex);

        buildRowImpl(rowIndex, rowValue, tr);
    }

    private void setRowValueIndex(TableRowElement tr, int rowIndex) {
        tr.setPropertyInt(ROW_ATTRIBUTE, rowIndex + 1);
    }

    public final int getRowValueIndex(Element row) {
        return row.getPropertyInt(ROW_ATTRIBUTE) - 1;
    }

    /**
     * Build zero or more table rows for the specified row value.
     *
     * @param rowIndex the absolute row index
     * @param rowValue    the value for the row to render
     * @param rowElement
     */
    protected abstract void buildRowImpl(int rowIndex, T rowValue, TableRowElement rowElement);

    /**
     * Build zero or more table rows for the specified row value.
     * @param rowIndex the absolute row index
     * @param rowValue    the value for the row to render
     * @param columnsToRedraw
     * @param tr
     */
    public void updateRow(int rowIndex, T rowValue, int[] columnsToRedraw, TableRowElement tr) {
        setRowValueIndex(tr, rowIndex);
        updateRow(rowIndex, rowValue, columnsToRedraw, tr, null);
    }

    public abstract void updateRow(int rowIndex, T rowValue, int[] columnsToRedraw, TableRowElement rowElement, BiPredicate<Column<T, ?>, Cell> filter);

    public void updateRowStickyLeft(TableSectionElement tbodyElement, List<Integer> stickyColumns, List<Integer> stickyLefts) {
        int rowCount = tbodyElement.getChildCount();
        if (rowCount > 0) {
            NodeList<TableRowElement> rows = tbodyElement.getRows();
            for (int i = 0; i < rowCount; ++i) {
                updateRowStickyLeftImpl(rows.getItem(i), stickyColumns, stickyLefts);
            }
        }
    }

    protected abstract void updateRowStickyLeftImpl(TableRowElement rowElement, List<Integer> stickyColumns, List<Integer> stickyLefts);

    protected final <C> void renderCell(TableCellElement td, Cell cell, Column<T, C> column) {
        td.setPropertyObject(COLUMN_ATTRIBUTE, column);
        column.renderDom(cell, td);

        if(column.isSticky()) {
            //class dataGridStickyCell is also used in DataGrid isStickyCell()
            td.addClassName("dataGridStickyCell");
            td.addClassName("background-inherit");
            td.getStyle().setProperty("position", "sticky"); // we need to add it explicitly since it is used in setupFillParent
        }
    }

    public final Column<T, ?> getColumn(Element elem) {
        return (Column<T, ?>) elem.getPropertyObject(COLUMN_ATTRIBUTE);
    }

    protected final <C> void updateCell(TableCellElement cellParent, Cell cell, Column<T, C> column) {
        column.updateDom(cell, cellParent);
    }

    public static void updateColors(Element element, String backgroundColor, String foregroundColor) {
        GFormController.setBackgroundColor(element, backgroundColor);
        GFormController.setForegroundColor(element, foregroundColor);
    }

    public static void clearColors(Element element) {
        GFormController.setBackgroundColor(element, null);
        GFormController.setForegroundColor(element, null);
    }
}
