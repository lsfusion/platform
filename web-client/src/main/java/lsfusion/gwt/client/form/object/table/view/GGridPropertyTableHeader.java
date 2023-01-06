package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;

import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
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
    private String formPath;
    private final TooltipManager.TooltipHelper toolTipHelper;

    private boolean notNull;
    private boolean hasChangeAction;

    private GSize headerHeight;

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
                return table.getWidget().isAttached() && table.getWidget().isVisible();
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public String getCreationPath() {
                return creationPath;
            }

            @Override
            public String getFormPath() {
                return formPath;
            }
        };
    }

    public void setPaths(String path, String creationPath, String formPath) {
        this.path = path;
        this.creationPath = creationPath;
        this.formPath = formPath;
    }

    public void setCaption(String caption, boolean notNull, boolean hasChangeAction) {
        this.caption = caption;
        this.notNull = notNull;
        this.hasChangeAction = hasChangeAction;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void setHeaderHeight(GSize headerHeight) {
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
            th.addClassName("background-inherit");
        }

        PropertyPanelRenderer.setStyles(th, notNull, hasChangeAction);
    }

    public final static GSize DEFAULT_HEADER_HEIGHT = GSize.CONST(34);

    public static Element renderTD(Element th, boolean defaultHeaderHeight, Boolean sortDir, String caption) {
        return renderTD(th, defaultHeaderHeight ? DEFAULT_HEADER_HEIGHT : null, sortDir, caption, true);
    }

    public static Element renderTD(Element th, GSize height, Boolean sortDir, String caption, boolean tableToExcel) {
//        if(height != null)
//            GPropertyTableBuilder.setRowHeight(th, height, tableToExcel);

        th = GwtClientUtils.wrapDiv(th); // we need to wrap in div, since we don't want to modify th itself (it's not recreated every time for grid) + setting display flex for th breaks layouting + for th it's unclear how to make it clip text that doesn't fit height (even max-height)
        th.addClassName("prop-header");

        // since it's a header we want to align it to the center (vertically and horizontally)
        th = wrapCenter(th); // we have to do it after setting height (because that's the point of that centering)

        // we don't want that container to be larger than the upper one
        // it seems it is needed because in wrapDiv we use auto sizing
        if(height != null)
            th.getStyle().setProperty("maxHeight", height.getString());

        th = wrapSortImg(th, sortDir);
//            th = wrapAlignedFlexImg(th, imgProcessor);

        th.addClassName("dataGridHeaderCell-caption"); // wrap normal to have multi-line headers

        renderCaption(th, caption);

        return th;
    }

    public static Element wrapCenter(Element th) {
        th.addClassName("wrap-center"); // display flex : justify-content, align-items : center

        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapSortImg(Element th, Boolean sortDir) {
        if(sortDir == null)
            return th;

        th.addClassName("wrap-wrapimgdiv");
        th.addClassName("wrap-text-not-empty");

        Element img = (sortDir ? StaticImage.SORTUP : StaticImage.SORTDOWN).createImage();
        img.addClassName("sort-img");
        img.addClassName("wrap-img-horz-margins");
        img.addClassName("wrap-img");
        th.appendChild(img);

        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("wrap-imgdiv");
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    private static void renderCaption(Element captionElement, String caption) {
        GwtClientUtils.setInnerContent(captionElement, caption);
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