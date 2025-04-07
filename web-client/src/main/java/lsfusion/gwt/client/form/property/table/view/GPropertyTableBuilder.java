package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.RowIndexHolder;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.List;
import java.util.function.BiPredicate;

import static lsfusion.gwt.client.view.MainFrame.v5;

/**
 * Based on lsfusion.gwt.client.base.view.grid.DefaultDataGridBuilder
 */
public abstract class GPropertyTableBuilder<T> extends AbstractDataGridBuilder<T> {

    public GPropertyTableBuilder(DataGrid table) {
        super(table);
    }

    // when we have a td and it is not a simple text, we have to wrap it, because td has a display : table-cell (and often incorrect element type in general)
    // and thus height for example always work like min-height, so the size depends on
    // and it can not be changed and it's behaviour is often very odd
    private static boolean needWrap(Element element, GPropertyDraw property, RendererType rendererType) {
        return GwtClientUtils.isTDorTH(element) && !property.getCellRenderer(rendererType).canBeRenderedInTD();
    }

    // pretty similar to GGridPropertyTableHeader.renderTD
    public static Element renderSized(Element element, GPropertyDraw property, GFont font, RendererType rendererType) {
        // not td/th only in one very rare case - sorted last column value in pivot
        if(needWrap(element, property, rendererType)) {
            element = wrapSized(element, property.getCellRenderer(rendererType).createRenderElement(rendererType));
        }

        // we need to set the size to the "render" element to avoid problems with padding
        GSize valueHeight = property.getValueHeight(font, false, true);
        FlexPanel.setGridHeight(element, valueHeight);
        if(valueHeight != null) // this way we can avoid prop-size-value fill-parent-perc conflict (see the css file) in most cases
            GwtClientUtils.addClassName(element, "prop-size-value");

        return element;
    }

    public static Element getRenderSizedElement(Element element, GPropertyDraw property, RendererType rendererType) {
        if(needWrap(element, property, rendererType))
            element = unwrapSized(element);

        return element;
    }

    public static boolean clearRenderSized(Element element, GPropertyDraw property, RendererType rendererType) {
        if(needWrap(element, property, rendererType)) {
            GwtClientUtils.removeAllChildren(element);

            return true;
        }

        GwtClientUtils.removeClassName(element, "prop-class-value");

        FlexPanel.setGridHeight(element, (GSize)null);

        return false;
    }

    public static Element wrapSized(Element element, Element renderElement) {
//        assert GwtClientUtils.isTDorTH(element);
        // the thing is that td ignores min-height (however height in td works just like min-height)
        // and we want height in table div work as min-height (i.e. to stretch)
        element.appendChild(renderElement);
        GwtClientUtils.setupPercentParent(renderElement);
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
        RendererType rendererType = renderContext.getRendererType();
        property.getCellRenderer(rendererType).render(renderSized(element, property, renderContext.getFont(), rendererType), renderContext);
    }

    // pivot (render&update), footer (render&update, update)
    public static void update(GPropertyDraw property, Element element, UpdateContext updateContext) {
        RendererType rendererType = updateContext.getRendererType();
        property.getCellRenderer(rendererType).update(getRenderSizedElement(element, property, rendererType), updateContext);
    }

    @Override
    public void buildRowImpl(int rowIndex, T rowValue, TableRowElement tr) {
        setRowValueIndex(tr, rowIndex, (RowIndexHolder) rowValue);

        buildRow(rowIndex, (RowIndexHolder) rowValue, tr);
    }

    @Override
    public void buildColumnRow(TableRowElement tr) {
        buildRow(-1, null, tr);

        GwtClientUtils.addClassName(tr, "data-grid-column-row", "dataGridColumnRow", v5);
    }

    private void buildRow(int rowIndex, RowIndexHolder rowValue, TableRowElement tr) {
        GwtClientUtils.addClassName(tr, "data-grid-row", "dataGridRow", v5);

        // Build the columns.
        int columnCount = cellTable.getColumnCount();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            Column<T, ?> column = cellTable.getColumn(columnIndex);

            TableCellElement td = tr.insertCell(-1);

            GwtClientUtils.addClassName(td, "data-grid-cell", "dataGridCell", v5);
            if (columnIndex == 0)
                GwtClientUtils.addClassName(td, "data-grid-first-cell", "dataGridFirstCell", v5);
            if (columnIndex == columnCount - 1)
                GwtClientUtils.addClassName(td, "data-grid-last-cell", "dataGridLastCell", v5);

            renderTD(td, false);

            Cell cell = new Cell(rowIndex, columnIndex, column, rowValue);

            renderCell(td, cell, column);

            if(cellTable.isColumnFlex(columnIndex)) {
                if(rowIndex >= 0)
                    td.setColSpan(2);
                else {
                    TableCellElement flexTd = tr.insertCell(-1);
                    GwtClientUtils.addClassName(flexTd, "data-grid-column-row-flex-cell", "dataGridColumnRowFlexCell", v5);
                    GwtClientUtils.addClassName(flexTd, "remove-all-pmb");
                }
            }

            if(rowIndex >= 0)
                updateCell(td, cell, column);
            else
                GwtClientUtils.addClassName(td, "data-grid-column-row-pref-cell", "dataGridColumnRowPrefCell", v5);
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
    public void updateRowStickyLeftImpl(TableRowElement tr, List<Integer> stickyColumns, List<DataGrid.StickyParams> stickyLefts) {
        updateStickyLeft(tr, stickyColumns, stickyLefts, false);
    }

    private static native void setCellBorderRight(Element element, String borderRight) /*-{
        element.style.setProperty("--cell-border-right", borderRight);
    }-*/;
    private static native void clearCellBorderRight(Element element) /*-{
        element.style.removeProperty("--cell-border-right");
    }-*/;

    public static void updateStickyLeft(TableRowElement tr, List<Integer> stickyColumns, List<DataGrid.StickyParams> stickyLefts, boolean header) {
        for (int i = 0; i < stickyColumns.size(); i++) {
            Integer stickyColumn = stickyColumns.get(i);
            DataGrid.StickyParams left = stickyLefts.get(i);
            TableCellElement cell = tr.getCells().getItem(stickyColumn);
            if (left != null) {
                cell.getStyle().setProperty("left", left.left + "px");
                setCellBorderRight(cell, left.borderRight + "px");
                //if (!header) {
                    GwtClientUtils.removeClassName(cell, "data-grid-sticky-overflow");
                //}
            } else {
                cell.getStyle().clearProperty("left");
                clearCellBorderRight(cell);
                //if (!header) {
                    GwtClientUtils.addClassName(cell, "data-grid-sticky-overflow", "dataGridStickyOverflow", v5);
                //}
            }
        }
    }

    @Override
    public void updateStickedStateImpl(TableRowElement tr, List<Integer> stickyColumns, int lastSticked) {
        updateStickyCellsClasses(tr, stickyColumns, lastSticked);
    }
    
    public static void updateStickyCellsClasses(TableRowElement tr, List<Integer> stickyColumns, int lastSticked) {
        for (int i = 0; i < stickyColumns.size(); i++) {
            Integer stickyColumn = stickyColumns.get(i);
            TableCellElement cell = tr.getCells().getItem(stickyColumn);
            if(i == lastSticked)
                GwtClientUtils.addClassName(cell, "last-sticked");
            else
                GwtClientUtils.removeClassName(cell, "last-sticked");
            if (i <= lastSticked)
                GwtClientUtils.addClassName(cell, "sticked");
            else
                GwtClientUtils.removeClassName(cell, "sticked");
        }
    }

    // pivoting
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

}

