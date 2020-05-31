package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

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

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip) {
        super(DBLCLICK, MOUSEDOWN, MOUSEMOVE, MOUSEOVER, MOUSEOUT);

        this.caption = caption;
        this.table = table;
        this.toolTip = toolTip;
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

    @Override
    public void renderDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        renderedCaptionElement = renderTD(th, headerHeight, sortDir, caption);
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
        th = wrapDiv(th); // we need to wrap in div, since we don't want to modify th itself (it's not recreated every time for grid) + setting display flex for th breaks layouting

        int setHeight = height > 0 ? height : DEFAULT_HEADER_HEIGHT;
        GPropertyTableBuilder.setRowHeight(th, setHeight);

        // since it's a header we want to align it to the center (vertically and horizontally)
        th = wrapCenter(th); // we have to do it after setting height (because that's the point of that centering)
        // we don't want that container to be larger than the upper one
        th.getStyle().setProperty("maxHeight", setHeight + "px");

        if(sortDir != null)
            th = wrapSort(th, sortDir);

        th.addClassName("dataGridHeaderCell-caption"); // wrap normal to have multi-line headers
        renderCaption(th, caption);

        return th;
    }

    //  will wrap with div, because otherwise other wrappers will add and not remove classes after update
    public static Element wrapDiv(Element th) {
        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("dataGridHeaderCell-div");
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapCenter(Element th) {
        th.addClassName("dataGridHeaderCell-wrapcenter"); // display flex : justify-content stretch, align-items

        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapSort(Element th, Boolean sortDir) {
        th.addClassName("dataGridHeaderCell-wrapsortdiv");

        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("dataGridHeaderCell-sortdiv");

        ImageElement img = Document.get().createImageElement();
        img.addClassName("dataGridHeaderCell-sortimg");
        GwtClientUtils.setThemeImage(sortDir ? "arrowup.png" : "arrowdown.png", img::setSrc);
        th.appendChild(img);

        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    private static void renderCaption(Element captionElement, String caption) {
        captionElement.setInnerText(caption == null ? "" : EscapeUtils.unicodeEscape(caption));
    }

    @Override
    public void updateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        if (!nullEquals(sortDir, renderedSortDir)) {
            GwtClientUtils.removeAllChildren(th);
            renderDom(th);
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