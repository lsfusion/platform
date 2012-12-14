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

import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.client.*;
import platform.gwt.base.client.JSNHelper;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.Cell.Context;
import platform.gwt.cellview.client.cell.HasCell;

import java.util.*;

import static java.lang.Math.min;
import static platform.gwt.base.client.GwtClientUtils.removeAllChildren;

/**
 * Builder used to construct a CellTable.
 *
 * @param <T> the row data type
 */
public abstract class AbstractDataGridBuilder<T> implements CellTableBuilder<T> {

    /**
     * The attribute used to indicate that an element contains a cell.
     */
    private static final String CELL_ATTRIBUTE = "__gwt_cell";

    private static final String CELL_TYPE_ATTRIBUTE = "__gwt_celltype";

    /**
     * The attribute used to specify the logical row index.
     */
    private static final String ROW_ATTRIBUTE = "__gwt_row";

    protected final DataGrid<T> cellTable;

    /**
     * A mapping of unique cell IDs to the cell.
     */
    private final Map<String, HasCell<T, ?>> idToCellMap = new HashMap<String, HasCell<T, ?>>();
    private final Map<HasCell<T, ?>, String> cellToIdMap = new HashMap<HasCell<T, ?>, String>();

    private TableSectionElement tbodyElement;

    /**
     * Construct a new table builder.
     *
     * @param cellTable the table this builder will build rows for
     */
    public AbstractDataGridBuilder(DataGrid<T> cellTable) {
        this.cellTable = cellTable;
        tbodyElement = cellTable.getTableBodyElement();
    }

    /**
     * Return the column containing an element.
     *
     * @param context  the context for the element
     * @param rowValue the value for the row corresponding to the element
     * @param elem     the element that the column contains
     * @return the immediate column containing the element
     */
    @Override
    public final HasCell<T, ?> getColumn(Context context, T rowValue, Element elem) {
        return getColumn(elem);
    }

    /**
     * Return all the columns that this table builder has renderred.
     */
    @Override
    public final Collection<HasCell<T, ?>> getColumns() {
        return idToCellMap.values();
    }

    /**
     * Get the index of the row value from the associated {@link TableRowElement}.
     *
     * @param row the row element
     * @return the row value index
     */
    @Override
    public final int getRowValueIndex(TableRowElement row) {
        try {
            return Integer.parseInt(row.getAttribute(ROW_ATTRIBUTE));
        } catch (NumberFormatException e) {
            // The attribute doesn't exist. Maybe the user is overriding
            // renderRowValues().
            return row.getSectionRowIndex();
        }
    }

    /**
     * Return if an element contains a cell. This may be faster to execute than {@link getColumn}.
     *
     * @param elem the element of interest
     */
    @Override
    public final boolean isColumn(Element elem) {
        return getCellId(elem) != null;
    }

    @Override
    public final void update(List<T> values, List<Range> updateRanges, int[] columnsToRedraw, boolean columnsChanged) {
        //assertion that updateRanges is sorted

        int valCount = values.size();

        NodeList<TableRowElement> rows = tbodyElement.getRows();
        int rowCount = rows.getLength();
        if (rowCount > valCount) {
            for (int i = rowCount - 1; i >= valCount; --i) {
                TableRowElement rowElement = rows.getItem(i);
                tbodyElement.removeChild(rowElement);
            }
            rowCount = valCount;
        }

        if (columnsChanged) {
            //rebuild rows if columns have been changed
            for (int i = 0; i < rowCount; ++i) {
                rebuildRow(i, values.get(i));
            }
        } else {
            //update changed rows
            for (Range range : updateRanges) {
                if (range.start > rowCount) {
                    break;
                }
                int end = min(rowCount, range.end);
                for (int i = range.start; i < end; ++i) {
                    updateRow(i, values.get(i), columnsToRedraw);
                }
            }
        }

        //build new rows
        for (int i = rowCount; i < valCount; ++i) {
            buildRow(values.get(i));
        }
    }

    private void rebuildRow(int rowIndex, T rowValue) {
        TableRowElement rowElement = tbodyElement.getRows().getItem(rowIndex);
        removeAllChildren(rowElement);

        buildRowImpl(rowIndex, rowValue, rowElement);
    }

    private void buildRow(T rowValue) {
        int rowIndex = tbodyElement.getRows().getLength();

        TableRowElement rowElement = tbodyElement.insertRow(rowIndex);
        rowElement.setAttribute(ROW_ATTRIBUTE, String.valueOf(rowIndex));

        buildRowImpl(rowIndex, rowValue, rowElement);
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
     *
     * @param rowIndex the absolute row index
     * @param rowValue    the value for the row to render
     * @param columnsToRedraw
     */
    private void updateRow(int rowIndex, T rowValue, int[] columnsToRedraw) {
        TableRowElement rowElement = tbodyElement.getRows().getItem(rowIndex);
        updateRowImpl(rowIndex, rowValue, rowElement, columnsToRedraw);
    }

    protected abstract void updateRowImpl(int rowIndex, T rowValue, TableRowElement rowElement, int[] columnsToRedraw);

    /**
     * Render the cell into an {@link ElementBuilderBase}.
     *
     * @param cellParent  the {@link com.google.gwt.dom.builder.shared.ElementBuilderBase} that cell contents append to
     * @param context  the context for the element
     * @param column   the column containing the cell
     * @param rowValue the value for the row corresponding to the element
     */
    protected final <C> void renderCell(DivElement cellParent, Context context, HasCell<T, C> column, T rowValue) {
        // Generate a unique ID for the cell.
        String cellId = cellToIdMap.get(column);
        if (cellId == null) {
            cellId = "cell-" + Document.get().createUniqueId();
            idToCellMap.put(cellId, column);
            cellToIdMap.put(column, cellId);
        }
        cellParent.setAttribute(CELL_ATTRIBUTE, cellId);

        Cell<C> cell = column.getCell();
        String cellType = cell.getCellType(context);
        if (cellType != null) {
            cellParent.setAttribute(CELL_TYPE_ATTRIBUTE, cellType);
        }

        // Render the cell into the DOM
        cell.renderDom(context, cellParent, column.getValue(rowValue));
    }

    protected final <C> void updateCell(DivElement cellParent, Context context, HasCell<T, C> column, T rowValue) {
        Cell<C> cell = column.getCell();

        String oldCellType = JSNHelper.getAttributeOrNull(cellParent, CELL_TYPE_ATTRIBUTE);
        String newCellType = cell.getCellType(context);
        if (oldCellType != null || newCellType != null) {
            //if types doesn't match, than don't update - render instead
            if (oldCellType == null || !oldCellType.equals(newCellType)) {
                removeAllChildren(cellParent);
                if (newCellType == null) {
                    cellParent.removeAttribute(CELL_TYPE_ATTRIBUTE);
                }
                renderCell(cellParent, context, column, rowValue);
                return;
            }
        }

        cell.updateDom(context, cellParent, column.getValue(rowValue));
    }

    /**
     * Check if an element is the parent of a rendered cell.
     *
     * @param elem the element to check
     * @return the cellId if a cell parent, null if not
     */
    private String getCellId(Element elem) {
        if (elem == null) {
            return null;
        }
        return JSNHelper.getAttributeOrNull(elem, CELL_ATTRIBUTE);
    }

    /**
     * Return the column containing an element.
     *
     * @param elem the elm that the column contains
     * @return the column containing the element.
     */
    private HasCell<T, ?> getColumn(Element elem) {
        String cellId = getCellId(elem);
        return (cellId == null) ? null : idToCellMap.get(cellId);
    }
}
