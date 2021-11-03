package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {

    private final GGridPropertyTable table;

    private String renderedCaption;
    private Boolean renderedSortDir;

    private Element renderedCaptionElement;

    private String caption;
    private String toolTip;
    private String path;
    private String creationPath;
    private final TooltipManager.TooltipHelper toolTipHelper;

    private boolean notNull;
    private boolean hasChangeAction;

    private int headerHeight;

    private boolean sticky;

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip, boolean sticky) {
        this.caption = caption;
        this.table = table;
        this.toolTip = toolTip;
        this.sticky = sticky;

        toolTipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return GGridPropertyTableHeader.this.toolTip;
            }

            @Override
            public boolean stillShowTooltip() {
                return table.isAttached() && table.isVisible();
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public String getCreationPath() {
                return creationPath;
            }
        };
    }

    public void setPaths(String path, String creationPath) {
        this.path = path;
        this.creationPath = creationPath;
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
        Supplier<Integer> childIndex = () -> table.getHeaderIndex(this);

        if (DBLCLICK.equals(event.getType())) {
            stopPropagation(event);
            table.headerClicked(childIndex.get(), event.getCtrlKey(), event.getShiftKey());
        }

        ResizeHandler.checkResizeEvent(table.resizeHelper, table.getTableHeadElement(), childIndex, event);

        TooltipManager.checkTooltipEvent(event, toolTipHelper);
    }

    @Override
    public void renderAndUpdateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        renderedCaptionElement = renderTD(th, headerHeight, sortDir, caption, false);
        renderedSortDir = sortDir;
        renderedCaption = caption;

        if(sticky) {
            th.addClassName("dataGridStickyHeader");
        }

        if (notNull) {
            th.addClassName("dataGridRelative");
            DivElement notNullSign = Document.get().createDivElement();
            notNullSign.addClassName("rightBottomCornerTriangle");
            notNullSign.addClassName("notNullCornerTriangle");
            th.appendChild(notNullSign);
        } else if (hasChangeAction) {
            th.addClassName("dataGridRelative");
            DivElement changeActionSign = Document.get().createDivElement();
            changeActionSign.addClassName("rightBottomCornerTriangle");
            changeActionSign.addClassName("changeActionCornerTriangle");
            th.appendChild(changeActionSign);
        }
    }

    private final static int DEFAULT_HEADER_HEIGHT = 34;

    public static Element renderTD(Element th, Boolean sortDir, String caption) {
        return renderTD(th, DEFAULT_HEADER_HEIGHT, sortDir, caption, true);
    }

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

}