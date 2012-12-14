package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.Header;

import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;
import static platform.gwt.base.shared.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {
    private static final int ANCHOR_WIDTH = 10;

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
                        Column leftColumn;
                        Column rightColumn;
                        int initialMouseX;
                        int scaleWidth;
                        int scalePixelWidth = target.getOffsetWidth();
                        if (mouseX > anchorRight) {
                            leftColumn = table.getColumn(headerIndex);
                            rightColumn = table.getColumn(headerIndex + 1);
                            initialMouseX = target.getAbsoluteRight();
                            scaleWidth = getColumnWidth(leftColumn);
                        } else {
                            leftColumn = table.getColumn(headerIndex - 1);
                            rightColumn = table.getColumn(headerIndex);
                            initialMouseX = target.getAbsoluteLeft();
                            scaleWidth = getColumnWidth(rightColumn);
                        }
                        resizeHelper = new ColumnResizeHelper(leftColumn, rightColumn, initialMouseX, scaleWidth, scalePixelWidth);
                        stopPropagation(event);
                    }
                } else {
                    target.getStyle().setCursor(Cursor.DEFAULT);
                }
            }
        }
    }

    private int getColumnWidth(Column column) {
        String width = table.getColumnWidth(column);
        return Integer.parseInt(width.substring(0, width.indexOf("px")));
    }

    @Override
    public void renderDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);
        String escapedCaption = getEscapedCaption();

        if (sortDir != null) {
            DivElement div = Document.get().createDivElement();
            div.setTitle(escapedCaption);

            ImageElement img = Document.get().createImageElement();
            img.getStyle().setFloat(Style.Float.LEFT);
            img.getStyle().setHeight(15, Style.Unit.PX);
            img.getStyle().setWidth(15, Style.Unit.PX);
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
            DivElement div = Document.get().createDivElement();
            div.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            div.setTitle(escapedCaption);
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
        private Column rightColumn;

        private int scaleWidth;
        private int scalePixelWidth;

        private int leftInitialWidth;
        private int rightInitialWidth;

        public ColumnResizeHelper(Column leftColumn, Column rightColumn, int initalMouseX, int scaleWidth, int scalePixelWidth) {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
            this.initalMouseX = initalMouseX;
            this.scaleWidth = scaleWidth;
            this.scalePixelWidth = scalePixelWidth;

            leftInitialWidth = getColumnWidth(leftColumn);
            rightInitialWidth = getColumnWidth(rightColumn);

            previewHandlerReg = Event.addNativePreviewHandler(this);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            stopPropagation(nativeEvent);
            if (nativeEvent.getType().equals("mousemove")) {
                resizeHeaders(nativeEvent.getClientX());
            } else if (nativeEvent.getType().equals("mouseup")) {
                previewHandlerReg.removeHandler();
                resizeHelper = null;
            }
        }

        private void resizeHeaders(int clientX) {
            int dragX = clientX - initalMouseX;

            int dragColumnWidth = dragX * scaleWidth / scalePixelWidth;

            if (leftInitialWidth + dragColumnWidth > 0 && rightInitialWidth - dragColumnWidth > 0) {
                table.setColumnWidth(leftColumn, (leftInitialWidth + dragColumnWidth) + "px");
                table.setColumnWidth(rightColumn, (rightInitialWidth - dragColumnWidth) + "px");
            }
        }
    }
}