package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.function.Consumer;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;
import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;
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
    private TooltipManager.TooltipHelper toolTipHandler;

    private boolean notNull;
    private boolean hasChangeAction;

    private int headerHeight;

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip) {
        this.caption = caption;
        this.table = table;
        this.toolTip = toolTip;

        toolTipHandler = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return GGridPropertyTableHeader.this.toolTip;
            }

            @Override
            public boolean stillShowTooltip() {
                return table.isAttached() && table.isVisible();
            }
        };
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
            table.headerClicked(this, event.getCtrlKey(), event.getShiftKey());
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
        }

        TooltipManager.checkTooltipEvent(event, toolTipHandler);
    }

    @Override
    public void renderAndUpdateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        renderedCaptionElement = renderTD(th, headerHeight, sortDir, caption, false);
        renderedSortDir = sortDir;
        renderedCaption = caption;

        if (notNull) {
            th.getStyle().setPosition(Style.Position.RELATIVE);
            DivElement notNullSign = Document.get().createDivElement();
            notNullSign.addClassName("rightBottomCornerTriangle");
            notNullSign.addClassName("notNullCornerTriangle");
            th.appendChild(notNullSign);
        } else if (hasChangeAction) {
            th.getStyle().setPosition(Style.Position.RELATIVE);
            DivElement changeActionSign = Document.get().createDivElement();
            changeActionSign.addClassName("rightBottomCornerTriangle");
            changeActionSign.addClassName("changeActionCornerTriangle");
            th.appendChild(changeActionSign);
        }
    }

    private final static int DEFAULT_HEADER_HEIGHT = 34;

    public static Element renderTD(Element th, int height, Boolean sortDir, String caption) {
        return renderTD(th, height, sortDir, caption, true);
    }

    public static Element renderTD(Element th, int height, Boolean sortDir, String caption, boolean tableToExcel) {
        int setHeight = height >= 0 ? height : DEFAULT_HEADER_HEIGHT;
        GPropertyTableBuilder.setRowHeight(th, setHeight, tableToExcel);

        th = GwtClientUtils.wrapCenteredImg(th, true, setHeight, getSortImgProcesspr(sortDir));
        th.addClassName("dataGridHeaderCell-caption"); // wrap normal to have multi-line headers
        renderCaption(th, caption);

        return th;
    }

    public static Consumer<ImageElement> getSortImgProcesspr(Boolean sortDir) {
        return sortDir != null ? img -> {
            img.addClassName("dataGridHeaderCell-sortimg");
            GwtClientUtils.setThemeImage(sortDir ? "arrowup.png" : "arrowdown.png", img::setSrc);
        } : null;
    }

    public static void changeDirection(ImageElement img, boolean sortDir) {
        GwtClientUtils.setThemeImage(sortDir ? "arrowup.png" : "arrowdown.png", img::setSrc);
    }

    private static void renderCaption(Element captionElement, String caption) {
        captionElement.setInnerHTML(caption == null ? "" : escapeLineBreakHTML(caption));
    }

    @Override
    public void updateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        if (!nullEquals(sortDir, renderedSortDir)) {
            GwtClientUtils.removeAllChildren(th);
            renderAndUpdateDom(th);
        } else if (!nullEquals(this.caption, renderedCaption)) {
            renderCaption(renderedCaptionElement, caption);
            renderedCaption = caption;
        }
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