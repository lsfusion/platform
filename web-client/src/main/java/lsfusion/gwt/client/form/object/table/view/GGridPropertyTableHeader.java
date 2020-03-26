package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.base.view.grid.HeaderPanel;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {
    private static final int ANCHOR_WIDTH = 10;

    private final GGridPropertyTable table;

    private ColumnResizeHelper resizeHelper = null;

    private String renderedCaption;
    private Boolean renderedSortDir;

    private Element renderedCaptionElement;

    private String caption;
    private String toolTip;

    private boolean notNull;
    private boolean hasChangeAction;

    private int headerHeight;

    public GGridPropertyTableHeader(GGridPropertyTable table, int headerHeight) {
        this(table, null, null, headerHeight);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption) {
        this(table, caption, null);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip) {
        this(table, caption, toolTip, 0);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip, int headerHeight) {
        super(DBLCLICK, MOUSEDOWN, MOUSEMOVE, MOUSEOVER, MOUSEOUT);

        this.caption = caption;
        this.table = table;
        this.toolTip = toolTip;
        this.headerHeight = headerHeight;
    }

    public void setCaption(String caption, boolean notNull, boolean hasChangeAction) {
        this.caption = caption;
        this.notNull = notNull;
        this.hasChangeAction = hasChangeAction;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    @Override
    public void onBrowserEvent(Element target, NativeEvent event) {
        String eventType = event.getType();
        if (DBLCLICK.equals(eventType)) {
            stopPropagation(event);
            table.headerClicked(this, event.getCtrlKey());
        } else if (MOUSEMOVE.equals(eventType) || MOUSEDOWN.equals(eventType)) {
            if (resizeHelper == null) {
                int mouseX = event.getClientX();

                int anchorRight = target.getAbsoluteRight() - ANCHOR_WIDTH;
                int anchorLeft = target.getAbsoluteLeft() + ANCHOR_WIDTH;

                int headerIndex = table.getHeaderIndex(this);
                if ((mouseX > anchorRight && headerIndex != table.getColumnCount() - 1) || (mouseX < anchorLeft && headerIndex > 0)) {
                    target.getStyle().setCursor(Cursor.COL_RESIZE);
                    if (eventType.equals(MOUSEDOWN)) {
                        int leftColumnIndex = mouseX > anchorRight ? headerIndex : headerIndex - 1;
                        TableCellElement leftHeaderCell = ((TableRowElement) target.getParentElement()).getCells().getItem(leftColumnIndex);

                        resizeHelper = new ColumnResizeHelper(leftColumnIndex, leftHeaderCell);
                        stopPropagation(event);
                    }
                } else {
                    target.getStyle().setCursor(Cursor.DEFAULT);
                }
            }
        } else if (MOUSEOVER.equals(eventType)) {
            TooltipManager.get().showTooltip(event.getClientX(), event.getClientY(), new TooltipManager.TooltipHelper() {
                @Override
                public String getTooltip() {
                    return toolTip;
                }

                @Override
                public boolean stillShowTooltip() {
                    return table.isAttached() && table.isVisible();
                }
            });
        } else if (MOUSEOUT.equals(eventType)) {
            TooltipManager.get().hideTooltip();
        }
        if (MOUSEMOVE.equals(eventType)) {
            TooltipManager.get().updateMousePosition(event.getClientX(), event.getClientY());
        }
        if (MOUSEDOWN.equals(eventType)) {
            TooltipManager.get().hideTooltip();
        }
    }

    private double getColumnWidth(Column column) {
        String width = table.getColumnWidth(column);
        return Double.parseDouble(width.substring(0, width.indexOf("px")));
    }

    @Override
    public void renderDom(TableCellElement th) {
        th.addClassName("positionRelative");
        th.getStyle().setHeight(headerHeight >= 0 ? headerHeight : HeaderPanel.DEFAULT_HEADER_HEIGHT, Style.Unit.PX);

        Boolean sortDir = table.getSortDirection(this);
        String escapedCaption = getEscapedCaption();

        int maxHeight = headerHeight >= 0 ? headerHeight : HeaderPanel.DEFAULT_HEADER_HEIGHT;
        DivElement div = Document.get().createDivElement();
        div.getStyle().setProperty("maxHeight", maxHeight, Style.Unit.PX);
        div.getStyle().setOverflow(Style.Overflow.HIDDEN);
        div.getStyle().setTextAlign(Style.TextAlign.CENTER);
        div.getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);

        if (sortDir != null) {
            ImageElement img = Document.get().createImageElement();
            img.getStyle().setMarginTop(1, Style.Unit.PX);
            img.getStyle().setMarginRight(2, Style.Unit.PX);
            img.getStyle().setMarginBottom(1, Style.Unit.PX);
            img.getStyle().setMarginLeft(2, Style.Unit.PX);
            img.getStyle().setVerticalAlign(Style.VerticalAlign.BOTTOM);

            GwtClientUtils.setThemeImage(sortDir ? "arrowup.png" : "arrowdown.png", img::setSrc);

            SpanElement span = Document.get().createSpanElement();
            span.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            span.setInnerText(escapedCaption);

            renderedCaptionElement = span;

            div.appendChild(img);
            div.appendChild(span);

            th.appendChild(div);
        } else {
            div.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            div.setInnerText(escapedCaption);
            renderedCaptionElement = div;
            th.appendChild(div);
        }

        if (notNull) {
            DivElement notNullSign = Document.get().createDivElement();
            notNullSign.addClassName("rightBottomCornerTriangle");
            notNullSign.addClassName("notNullCornerTriangle");
            th.appendChild(notNullSign);
        } else if (hasChangeAction) {
            DivElement changeActionSign = Document.get().createDivElement();
            changeActionSign.addClassName("rightBottomCornerTriangle");
            changeActionSign.addClassName("changeActionCornerTriangle");
            th.appendChild(changeActionSign);
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
        TableCellElement leftHeaderCell;
        private int leftColumnIndex;

        public ColumnResizeHelper(int leftColumnIndex, TableCellElement leftHeaderCell) {
            this.leftHeaderCell = leftHeaderCell;
            this.initalMouseX = leftHeaderCell.getAbsoluteRight();
            this.leftColumnIndex = leftColumnIndex;

            previewHandlerReg = Event.addNativePreviewHandler(this);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            stopPropagation(nativeEvent);
            if (nativeEvent.getType().equals(MOUSEMOVE)) {
                int clientX = nativeEvent.getClientX();
                int tableLeft = table.getAbsoluteLeft();
                if (clientX >= tableLeft && clientX <= tableLeft + table.getOffsetWidth()) {
                    resizeHeaders(clientX);
                }
            } else if (nativeEvent.getType().equals(MOUSEUP)) {
                previewHandlerReg.removeHandler();
                resizeHelper = null;
            }
        }

        private void resizeHeaders(int clientX) {
            int dragX = clientX - initalMouseX;
            if (Math.abs(dragX) > 2) {
                table.resizeColumn(leftColumnIndex, dragX);
//                initalMouseX = leftHeaderCell.getAbsoluteRight();
                initalMouseX = Math.max(clientX, leftHeaderCell.getAbsoluteRight()); // делается max, чтобы при resize'е влево растягивание шло с момента когда курсор вернется на правый край колонки (вправо там другие проблемы)
            }
        }
    }
}