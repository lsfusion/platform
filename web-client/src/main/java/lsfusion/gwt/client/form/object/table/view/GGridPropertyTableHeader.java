package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {

    private final GGridPropertyTable table;

    private String renderedCaption;
    private String renderedCaptionElementClass;
    private AppBaseImage renderedImage;
    private String renderedTooltip;
    private Boolean renderedSortDir;

    private Element renderedCaptionElement;

    private String caption;
    private String captionElementClass;
    private AppBaseImage image;
    private String tooltip;
    private String path;
    private String creationPath;
    private String formPath;
    protected JavaScriptObject tippy = null;
    private final TooltipManager.TooltipHelper tooltipHelper;

    private boolean notNull;
    private boolean hasChangeAction;

    private GSize headerHeight;

    private boolean sticky;
    private boolean hasDynamicImage;
    private boolean hasDynamicCaption;

    public GGridPropertyTableHeader(GGridPropertyTable table, GPropertyDraw property, GGridPropertyTable.GridPropertyColumn column) {
        this(table, null, null, null, null, column.isSticky(), property.hasDynamicImage(), property.hasDynamicCaption());
    }
    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String captionElementClass, AppBaseImage image, String tooltip, boolean sticky, boolean hasDynamicImage, boolean hasDynamicCaption) {
        this.caption = caption;
        this.captionElementClass = captionElementClass;
        this.image = image;
        this.table = table;
        this.tooltip = tooltip;
        this.sticky = sticky;
        this.hasDynamicImage = hasDynamicImage;
        this.hasDynamicCaption = hasDynamicCaption;

        tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip(String dynamicTooltip) {
                return nvl(dynamicTooltip, GGridPropertyTableHeader.this.tooltip);
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

    public void setCaptionElementClass(String captionElementClass) {
        this.captionElementClass = captionElementClass;
    }

    public void setImage(AppBaseImage image) {
        this.image = image;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
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

        TableSectionElement cursorElement = table.getTableHeadElement();

        ResizeHandler.dropCursor(cursorElement, event);

        ResizeHandler.checkResizeEvent(table.resizeHelper, cursorElement, childIndex, event);
    }

    @Override
    public void renderAndUpdateDom(TableCellElement th, boolean rerender) {
        Boolean sortDir = table.getSortDirection(this);

        this.renderedCaptionElement = renderTD(th, rerender, headerHeight, sortDir, caption, captionElementClass, image, false, hasDynamicImage, hasDynamicCaption);
        if(!rerender)
            tippy = TooltipManager.initTooltip(new PopupOwner(table.getPopupOwnerWidget(), th), tooltipHelper);
        renderedSortDir = sortDir;
        renderedCaption = caption;
        renderedCaptionElementClass = captionElementClass;
        renderedImage = image;

        if(sticky) {
            th.addClassName("dataGridStickyHeader");
            th.addClassName("background-inherit");
        }

        PropertyPanelRenderer.setStyles(th, notNull, hasChangeAction);
    }

    //  will wrap with div, because otherwise other wrappers will add and not remove classes after update
    public static Element wrapDiv(Element th) {
        return GPropertyTableBuilder.wrapSized(th, Document.get().createDivElement());
    }

    public final static GSize DEFAULT_HEADER_HEIGHT = GSize.CONST(34);

    public static void renderTD(Element th, boolean rerender, boolean defaultHeaderHeight, Boolean sortDir, String caption) {
        renderTD(th, rerender, defaultHeaderHeight ? DEFAULT_HEADER_HEIGHT : null, sortDir, caption, null, null, true, false, false);
    }

    private static boolean needWrap(Element element, Boolean sortDir, String caption, boolean hasDynamicCaption, AppBaseImage image, boolean hasDynamicImage, ImageHtmlOrTextType textType, GSize height) {
        // we need to wrap in div, since we don't want to modify th itself (it's not recreated every time for grid) + setting display flex for th breaks layouting + for th it's unclear how to make it clip text that doesn't fit height (even max-height)

        if(!GwtClientUtils.isTDorTH(element))
            return false;

        if(sortDir != null)
            return true;

        if(image != null || hasDynamicImage)
            return true;

        if((caption != null && GwtClientUtils.containsHtmlTag(caption)) || hasDynamicCaption)
            return true;

        if(textType.isWrap() && height != null) // similar to TextBasedCellRenderer.canBeRenderedInTdCheck, height in td works as min-height
            return true;

        return false;
    }

    // pretty similar to GGridPropertyTableBuilder.renderSized
    public static Element renderTD(Element th, boolean rerender, GSize height, Boolean sortDir, String caption, String captionElementClass, AppBaseImage image, boolean tableToExcel, boolean hasDynamicImage, boolean hasDynamicCaption) {
        if(rerender) { // assert that property is the same (except order), so we don't clear (including clearFlexAlignment and clearTextAlignment) anything (however filling some props classes one more time, but it doesn't matter)
            GwtClientUtils.removeAllChildren(th);

            CellRenderer.clearRenderTextAlignment(th, "start", "end");

            FlexPanel.setGridHeight(th, null);

            th.removeClassName("caption-grid-header");
            GwtClientUtils.clearValueShrinkHorz(th, true, true);
        }

//        if(height != null)
//            GPropertyTableBuilder.setRowHeight(th, height, tableToExcel);
        ImageHtmlOrTextType textType = ImageHtmlOrTextType.GRID_CAPTION;

        if(needWrap(th, sortDir, caption, hasDynamicCaption, image, hasDynamicImage, textType, height)) {
            th = wrapDiv(th);

            CellRenderer.renderFlexAlignment(th, "start", "end");
        } else
            CellRenderer.renderTextAlignment(th, "start", "end");

        FlexPanel.setGridHeight(th, height);

        th.addClassName("caption-grid-header");

        GwtClientUtils.renderValueShrinkHorz(th, true, true);

        // if image or sortDir we still need to wrap image text, because there is a conflict between flexAlignment and wrap-img-* (including sortImg) css classes
        if(sortDir != null || image != null || hasDynamicImage)
            th = wrapImageText(th);

        if(sortDir != null)
            th = wrapSortImg(th, sortDir);

        BaseImage.initImageText(th, textType);
        renderCaption(th, caption);
        renderCaptionElementClass(th, captionElementClass);
        renderImage(th, image);

        return th;
    }

    public static Element wrapImageText(Element th) {
        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapSortImg(Element th, Boolean sortDir) {
        assert sortDir != null;

        // imaged text classes
        th.addClassName("wrap-text-not-empty");
        th.addClassName("wrap-img-horz");
        th.addClassName("wrap-img-start");

        Element img = (sortDir ? StaticImage.SORTUP : StaticImage.SORTDOWN).createImage();

        img.addClassName("wrap-text-img");
        th.appendChild(img);

        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        // extra classes
        img.addClassName("sort-img"); // needed for pivot hack
        wrappedTh.addClassName("sort-div"); // need to stretch if stretched

        return wrappedTh;
    }

    private static void renderCaption(Element captionElement, String caption) {
        BaseImage.updateText(captionElement, caption);
    }
    private static void renderCaptionElementClass(Element captionElement, String classes) {
        BaseImage.updateClasses(captionElement, classes);
    }
    private static void renderImage(Element captionElement, AppBaseImage image) {
        BaseImage.updateImage(image, captionElement);
    }

    @Override
    public void updateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        if (!nullEquals(sortDir, renderedSortDir)) {
            renderAndUpdateDom(th, true);
        } else {
            if (!nullEquals(this.caption, renderedCaption)) {
                renderCaption(renderedCaptionElement, caption);
                renderedCaption = caption;
            }
            if (!nullEquals(this.captionElementClass, renderedCaptionElementClass)) {
                renderCaptionElementClass(renderedCaptionElement, captionElementClass);
                renderedCaptionElementClass = captionElementClass;
            }
            if (!nullEquals(this.image, renderedImage)) {
                renderImage(renderedCaptionElement, image);
                renderedImage = image;
            }
            if (!nullEquals(this.tooltip, renderedTooltip)) {
                TooltipManager.updateContent(tippy, tooltipHelper, tooltip);
                renderedTooltip = tooltip;
            }
        }
    }

}