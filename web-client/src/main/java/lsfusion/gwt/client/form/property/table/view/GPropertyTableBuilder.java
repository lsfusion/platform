package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
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

    public static Element renderSized(Element element, GPropertyDraw property, RenderContext renderContext) {
        boolean isTDorTH = GwtClientUtils.isTDorTH(element);
        boolean isSimpleText = isTDorTH && property.getCellRenderer().isSimpleText(renderContext);

        boolean autoSize = property.autoSize && !isSimpleText;
        int height = property.getValueHeightWithPadding(renderContext.getFont());

        // the thing is that td ignores min-height (it matters only for not auto sized elements)
        // however height in td works just like min-height, but only in Chrome
        if(!autoSize) // it's not possible to do it for not auto sized element, since they have position absolute, and td doesn't respect their size
            FlexPanel.setBaseSize(element, true, height, isTDorTH);

        if(!isSimpleText) {
            element = wrapSized(element);
            GwtClientUtils.setupSizedParent(element, autoSize);

            if(autoSize)
                FlexPanel.setBaseSize(element, true, height, false);
        }
        return element;
    }

    public static Element getRenderSizedElement(Element element, GPropertyDraw property, UpdateContext updateContext) {
        boolean isTDorTH = GwtClientUtils.isTDorTH(element);
        boolean isSimpleText = isTDorTH && property.getCellRenderer().isSimpleText(updateContext);

        if(!isSimpleText) // there is another unwrapping in GPropertyTableBuilder, so it also should be kept consistent
            element = unwrapSized(element);

        return element;
    }

    public static boolean clearRenderSized(Element element, GPropertyDraw property, RenderContext renderContext) {
        boolean isTDorTH = GwtClientUtils.isTDorTH(element);
        boolean isSimpleText = isTDorTH && property.getCellRenderer().isSimpleText(renderContext);

        boolean autoSize = property.autoSize && !isSimpleText;

        if(!autoSize)
            FlexPanel.setBaseSize(element, true, null, isTDorTH); // the thing is that td ignores min-height (however height in td works just like min-height)

        if(!isSimpleText) {
            GwtClientUtils.removeAllChildren(element);

            if(!autoSize)
                GwtClientUtils.clearFillParentElement(element.getParentElement());

            return true;
        }

        return false;
    }

    private static Element wrapSized(Element element) {
//        assert GwtClientUtils.isTDorTH(element);
        Element wrappedTh = Document.get().createDivElement();
        element.appendChild(wrappedTh);
        return wrappedTh;
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
    public void updateRowStickyLeftImpl(TableRowElement tr, List<Integer> stickyColumns, List<Integer> stickyLefts) {
        updateStickyLeft(tr, stickyColumns, stickyLefts);
    }

    public static void updateStickyLeft(TableRowElement tr, List<Integer> stickyColumns, List<Integer> stickyLefts) {
        for (int i = 0; i < stickyColumns.size(); i++) {
            Integer stickyColumn = stickyColumns.get(i);
            Integer left = stickyLefts.get(i);
            TableCellElement cell = tr.getCells().getItem(stickyColumn);
            if (left != null) {
                cell.getStyle().setProperty("left", left + "px");
                cell.removeClassName("dataGridStickyOverflow");
            } else {
                cell.getStyle().clearProperty("left");
                cell.addClassName("dataGridStickyOverflow");
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

    public static void setRowHeight(Element td, int height, boolean tableToExcel) {
        if(tableToExcel) {
            GPivot.setTableToExcelRowHeight(td, height);
        }
        td.getStyle().setHeight(height, Style.Unit.PX);
    }
}

