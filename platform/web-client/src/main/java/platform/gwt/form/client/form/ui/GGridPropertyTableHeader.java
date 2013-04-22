package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.Header;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;

import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;
import static platform.gwt.base.shared.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {
    private static final int ANCHOR_WIDTH = 10;
    private static final int MAX_HEADER_HEIGHT = 30; // должна быть равна .dataGridHeaderCell { height: ...; }

    private final GGridPropertyTable table;

    private ColumnResizeHelper resizeHelper = null;

    private String renderedCaption;
    private Boolean renderedSortDir;

    private Element renderedCaptionElement;
    private Element renderedTooltipElement;

    private String caption;

    public GGridPropertyTableHeader(GGridPropertyTable table) {
        this(table, null);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption) {
        super("dblclick", "mousedown", "mousemove");

        this.caption = caption;
        this.table = table;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public void onBrowserEvent(Element target, NativeEvent event) {
        String eventType = event.getType();
        if ("dblclick".equals(eventType)) {
            stopPropagation(event);
            table.headerClicked(this, event.getCtrlKey());
        } else if ("mousemove".equals(eventType) || "mousedown".equals(eventType)) {
            if (resizeHelper == null) {
                int mouseX = event.getClientX();

                int anchorRight = target.getAbsoluteRight() - ANCHOR_WIDTH;
                int anchorLeft = target.getAbsoluteLeft() + ANCHOR_WIDTH;

                int headerIndex = table.getHeaderIndex(this);
                if ((mouseX > anchorRight && headerIndex != table.getColumnCount() - 1) || (mouseX < anchorLeft && headerIndex > 0)) {
                    target.getStyle().setCursor(Cursor.COL_RESIZE);
                    if (eventType.equals("mousedown")) {
                        int leftColumnIndex = mouseX > anchorRight ? headerIndex : headerIndex - 1;
                        Column leftColumn = table.getColumn(leftColumnIndex);
                        TableCellElement leftHeaderCell = ((TableRowElement) target.getParentElement()).getCells().getItem(leftColumnIndex);

                        int rightColumnsCount = table.getColumnCount() - leftColumnIndex - 1;
                        Column[] rightColumns = new Column[rightColumnsCount];
                        double[] rightScaleWidths = new double[rightColumnsCount];
                        for (int i = 1; i <= rightColumnsCount; i++) {
                            Column column = table.getColumn(leftColumnIndex + i);
                            rightColumns[i - 1] = column;
                            rightScaleWidths[i - 1] = getColumnWidth(column);
                        }

                        resizeHelper = new ColumnResizeHelper(leftColumn, rightColumns, rightScaleWidths, leftHeaderCell.getAbsoluteRight(),
                                getColumnWidth(leftColumn), leftHeaderCell.getOffsetWidth());
                        stopPropagation(event);
                    }
                } else {
                    target.getStyle().setCursor(Cursor.DEFAULT);
                }
            }
        }
    }

    private double getColumnWidth(Column column) {
        String width = table.getColumnWidth(column);
        return Double.parseDouble(width.substring(0, width.indexOf("px")));
    }

    @Override
    public void renderDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);
        String escapedCaption = getEscapedCaption();

        DivElement div = Document.get().createDivElement();
        div.getStyle().setProperty("maxHeight", MAX_HEADER_HEIGHT, Style.Unit.PX);
        div.getStyle().setOverflow(Style.Overflow.HIDDEN);
        div.getStyle().setTextAlign(Style.TextAlign.CENTER);
        div.getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
        div.setTitle(escapedCaption);
        if (sortDir != null) {
            ImageElement img = Document.get().createImageElement();
            img.getStyle().setHeight(15, Style.Unit.PX);
            img.getStyle().setWidth(15, Style.Unit.PX);
            img.getStyle().setVerticalAlign(Style.VerticalAlign.BOTTOM);
            img.setSrc(GWT.getModuleBaseURL() + "images/" + (sortDir ? "arrowup.png" : "arrowdown.png"));

            SpanElement span = Document.get().createSpanElement();
            span.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            span.setInnerText(escapedCaption);

            renderedTooltipElement = div;
            renderedCaptionElement = span;

            div.appendChild(img);
            div.appendChild(span);
            th.appendChild(div);
        } else {
            div.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            div.setInnerText(escapedCaption);

            renderedTooltipElement = renderedCaptionElement = div;
            th.appendChild(div);
        }

        setRendered(caption, sortDir);
    }

    private String getEscapedCaption() {
        return caption == null ? "" : EscapeUtils.unicodeEscape(caption);
    }

    @Override
    public void updateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        if (!nullEquals(sortDir, renderedSortDir)) {
            //пока не заморачиваемся с апдейтом DOMа при изменении сортировки... просто перерендериваем
            GwtClientUtils.removeAllChildren(th);
            renderDom(th);
        } else if (!nullEquals(this.caption, renderedCaption)) {

            String escapedCaption = getEscapedCaption();

            renderedCaptionElement.setInnerText(escapedCaption);
            renderedTooltipElement.setTitle(escapedCaption);
        }

        setRendered(caption, sortDir);
    }

    private void setRendered(String caption, Boolean sortDir) {
        renderedCaption = caption;
        renderedSortDir = sortDir;
    }

    private class ColumnResizeHelper implements NativePreviewHandler {
        private HandlerRegistration previewHandlerReg;

        private int initalMouseX;

        private Column leftColumn;
        private double leftInitialWidth;

        private double scaleWidth;
        private int scalePixelWidth;

        private Column[] rightColumns;
        private double[] rightInitialWidths;
        private double[] rightCoeffs;

        public ColumnResizeHelper(Column leftColumn, Column[] rightColumns, double[] rightScaleWidths, int initalMouseX, double scaleWidth, int scalePixelWidth) {
            this.leftColumn = leftColumn;
            this.rightColumns = rightColumns;
            this.initalMouseX = initalMouseX;
            this.scaleWidth = scaleWidth;
            this.scalePixelWidth = scalePixelWidth;

            leftInitialWidth = scaleWidth;

            double rightSum = 0.0;
            for (double rightScaleWidth : rightScaleWidths) {
                rightSum += rightScaleWidth;
            }
            rightInitialWidths = new double[rightColumns.length];
            rightCoeffs = new double[rightColumns.length];
            for (int i = 0; i < rightColumns.length; i++) {
                rightInitialWidths[i] = getColumnWidth(rightColumns[i]);
                rightCoeffs[i] = rightSum == 0.0 ? 0.0 : rightScaleWidths[i] / rightSum;
            }

            previewHandlerReg = Event.addNativePreviewHandler(this);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            stopPropagation(nativeEvent);
            if (nativeEvent.getType().equals("mousemove")) {
                int clientX = nativeEvent.getClientX();
                int tableLeft = table.getAbsoluteLeft();
                if (clientX >= tableLeft && clientX <= tableLeft + table.getOffsetWidth()) {
                    resizeHeaders(clientX);
                }
            } else if (nativeEvent.getType().equals("mouseup")) {
                previewHandlerReg.removeHandler();
                resizeHelper = null;
            }
        }

        private void resizeHeaders(int clientX) {
            int dragX = clientX - initalMouseX;
            double dragColumnWidth = dragX * scaleWidth / scalePixelWidth;
            double newLeftWidth = leftInitialWidth + dragColumnWidth;

            if (table.getTableDataScroller().getMaximumHorizontalScrollPosition() > 0) {
                GPropertyDraw property = table.getProperty(new Cell.Context(table.getKeyboardSelectedRow(), table.getColumnIndex(leftColumn), table.getKeyboardSelectedRowValue()));
                int propertyMinWidth = property != null ? property.getMinimumPixelWidth() : 0;
                int propertyMaxWidth = property != null ? property.getMaximumPixelWidth() : Integer.MAX_VALUE;
                if (property == null || (newLeftWidth >= propertyMinWidth && newLeftWidth <= propertyMaxWidth)) {
                    table.setColumnWidth(leftColumn, newLeftWidth + "px");
                    table.onResize();
                }
            } else {
                if (newLeftWidth > 0) {
                    table.setColumnWidth(leftColumn, newLeftWidth + "px");

                    for (int i = 0; i < rightColumns.length; i++) {
                        table.setColumnWidth(rightColumns[i], (rightInitialWidths[i] - (rightCoeffs[i] != 0.0 ? dragColumnWidth * rightCoeffs[i] : dragColumnWidth / rightCoeffs.length)) + "px");
                    }
                    table.onResize();
                }
            }
        }
    }
}