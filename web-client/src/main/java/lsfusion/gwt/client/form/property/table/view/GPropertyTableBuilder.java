package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.grid.*;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.object.table.grid.view.GPivot;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Based on lsfusion.gwt.client.base.view.grid.DefaultDataGridBuilder
 */
public abstract class GPropertyTableBuilder<T> extends AbstractDataGridBuilder<T> {
    private final String rowStyle;
    private final String cellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;

    public GPropertyTableBuilder(DataGrid table) {
        super(table);

        // Cache styles for faster access.
        GridStyle style = table.getStyle();
        rowStyle = style.dataGridRow();
        cellStyle = style.dataGridCell();
        firstColumnStyle = " " + style.dataGridFirstCell();
        lastColumnStyle = " " + style.dataGridLastCell();
    }

    // when we have a td and it is not a simple text, we have to wrap it, because td has a display : table-cell (and often incorrect element type in general)
    // and thus height for example always work like min-height, so the size depends on
    // and it can not be changed and it's behaviour is often very odd
    private static boolean needWrap(Element element, GPropertyDraw property, RenderContext renderContext) {
        return GwtClientUtils.isTDorTH(element) && !property.getCellRenderer().isSimpleText(renderContext);
    }
    private static boolean needWrap(Element element, GPropertyDraw property, UpdateContext updateContext) {
        return GwtClientUtils.isTDorTH(element) && !property.getCellRenderer().isSimpleText(updateContext);
    }

    public static Element renderSized(Element element, GPropertyDraw property, RenderContext renderContext) {
        if(needWrap(element, property, renderContext)) {
            element = wrapSized(element, property.getCellRenderer().createRenderElement(renderContext));

            // the thing is that td ignores min-height (however height in td works just like min-height)
            // and we want height in table div work as min-height (i.e. to stretch)
            element.addClassName("cell-div");
        }

        // we need to set the size to the "render" element to avoid problems with padding
        FlexPanel.setHeight(element, property.getValueHeight(renderContext.getFont(), false, true));

        return element;
    }

    public static Element getRenderSizedElement(Element element, GPropertyDraw property, UpdateContext updateContext) {
        if(needWrap(element, property, updateContext))
            element = unwrapSized(element);

        return element;
    }

    public static boolean clearRenderSized(Element element, GPropertyDraw property, RenderContext renderContext) {
        if(needWrap(element, property, renderContext)) {
            GwtClientUtils.removeAllChildren(element);

            return true;
        }

        FlexPanel.setHeight(element, (GSize)null);

        return false;
    }

    private static Element wrapSized(Element element, Element renderElement) {
//        assert GwtClientUtils.isTDorTH(element);
        element.appendChild(renderElement);
        return renderElement;
    }

    public static Element unwrapSized(Element element) {
//        assert GwtClientUtils.isTDorTH(element);
        return element.getFirstChildElement();
    }

    // pivot / footer
    public static void renderAndUpdate(GPropertyDraw property, Element element, RenderContext renderContext, UpdateContext updateContext) {
        // // can be div in renderColAttrCell : if + is added or sort
//        assert GwtClientUtils.isTDorTH(element);
        render(property, element, renderContext);
        update(property, element, updateContext);
    }

    // pivot / footer
    public static void render(GPropertyDraw property, Element element, RenderContext renderContext) {
        property.getCellRenderer().render(renderSized(element, property, renderContext), renderContext);
    }

    // pivot (render&update), footer (render&update, update)
    public static void update(GPropertyDraw property, Element element, UpdateContext updateContext) {
        property.getCellRenderer().update(getRenderSizedElement(element, property, updateContext), updateContext);
    }

    @Override
    public void buildRowImpl(int rowIndex, T rowValue, TableRowElement tr) {

        setRowValueIndex(tr, rowIndex, (RowIndexHolder) rowValue);
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

            TableCellElement td = tr.insertCell(-1); //columnIndex
            td.setClassName(tdClasses.toString());

            renderTD(td, false);

            Cell cell = new Cell(rowIndex, columnIndex, column, (RowIndexHolder) rowValue);

            renderCell(td, cell, column);

            updateCell(td, cell, column);
        }
    }

    @Override
    public void updateRowImpl(int rowIndex, T rowValue, int[] columnsToRedraw, TableRowElement tr, BiPredicate<Column<T, ?>, Cell> filter) {
        setRowValueIndex(tr, rowIndex, (RowIndexHolder) rowValue); // technically for updateSelectedCells it's not needed, but just in case

        int columnCount = cellTable.getColumnCount();

        assert columnCount == tr.getCells().getLength();

        if (columnsToRedraw == null) {
            if (columnCount > 0) {
                //td.nextSibling is a lot faster than cells[index]
                //http://jsperf.com/nextsibling-vs-childnodes
                TableCellElement td = tr.getFirstChild().cast();
                int columnIndex = 0;
                while (true) {
                    updateCellImpl(rowIndex, rowValue, td, columnIndex, filter);
                    if(++columnIndex >= columnCount)
                        break;
                    td = td.getNextSibling().cast();
                }
            }
        } else {
            NodeList<TableCellElement> cells = tr.getCells();
            for (int columnIndex : columnsToRedraw) {
                TableCellElement td = cells.getItem(columnIndex);
                updateCellImpl(rowIndex, rowValue, td, columnIndex, filter);
            }
        }
    }

    private void updateCellImpl(int rowIndex, T rowValue, TableCellElement td, int columnIndex, BiPredicate<Column<T, ?>, Cell> filter) {
        Column<T, ?> column = cellTable.getColumn(columnIndex);

        Cell cell = new Cell(rowIndex, columnIndex, column, (RowIndexHolder) rowValue);

        if(filter != null && !filter.test(column, cell))
            return;

        updateCell(td, cell, column);
    }

    @Override
    public void updateRowStickyLeftImpl(TableRowElement tr, List<Integer> stickyColumns, List<GSize> stickyLefts) {
        updateStickyLeft(tr, stickyColumns, stickyLefts, false);
    }

    public static void updateStickyLeft(TableRowElement tr, List<Integer> stickyColumns, List<GSize> stickyLefts, boolean header) {
        for (int i = 0; i < stickyColumns.size(); i++) {
            Integer stickyColumn = stickyColumns.get(i);
            GSize left = stickyLefts.get(i);
            TableCellElement cell = tr.getCells().getItem(stickyColumn);
            if (left != null) {
                cell.getStyle().setProperty("left", left.getString());
                if (!header) {
                    cell.removeClassName("dataGridStickyOverflow");
                }
            } else {
                cell.getStyle().clearProperty("left");
                if (!header) {
                    cell.addClassName("dataGridStickyOverflow");
                }
            }
        }
    }

    public static void renderTD(Element td) {
        renderTD(td, true);
    }

    public static void renderTD(Element td, boolean tableToExcel) {
//        setRowHeight(td, height, tableToExcel);
    }

    // setting line height to height it's the easiest way to align text to the center vertically, however it works only for single lines (which is ok for row data)
//    public static void setLineHeight(Element td, int height) {
//        td.getStyle().setLineHeight(height, Style.Unit.PX);
//    }
//    public static void clearLineHeight(Element td) {
//        td.getStyle().clearLineHeight();
//    }
    public static void setVerticalMiddleAlign(Element element) {
        element.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
    }

    public static void setRowHeight(Element td, GSize height, boolean tableToExcel) {
        if(tableToExcel) {
            GPivot.setTableToExcelRowHeight(td, height);
        }
        FlexPanel.setHeight(td, height);
    }
}

