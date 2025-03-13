package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;
import static lsfusion.gwt.client.form.property.cell.view.CellRenderer.renderTextAlignment;
import static lsfusion.gwt.client.view.MainFrame.v5;

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

    private boolean sticky;
    private GPropertyDraw property;

    public GGridPropertyTableHeader(GGridPropertyTable table, GPropertyDraw property, GGridPropertyTable.GridPropertyColumn column) {
        this(table, null, null, null, null, column.isSticky(), property);
    }
    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String captionElementClass, AppBaseImage image, String tooltip, boolean sticky, GPropertyDraw property) {
        this.caption = caption;
        this.captionElementClass = captionElementClass;
        this.image = image;
        this.table = table;
        this.tooltip = tooltip;
        this.sticky = sticky;
        this.property = property;

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

        this.renderedCaptionElement = renderTD(th, rerender,  sortDir, caption, captionElementClass, image, false, property, table.getUserHeaderHeight(), table.getGrid());
        if(!rerender) {
            tippy = TooltipManager.initTooltip(new PopupOwner(table.getPopupOwnerWidget(), th.getFirstChildElement()), tooltipHelper);
        }
        renderedSortDir = sortDir;
        renderedCaption = caption;
        renderedCaptionElementClass = captionElementClass;
        renderedImage = image;

        if(sticky) {
            GwtClientUtils.addClassName(th, "data-grid-sticky-header", "dataGridStickyHeader", v5);
            GwtClientUtils.addClassName(th, "background-inherit");
        }

        PropertyPanelRenderer.setStyles(th, notNull, hasChangeAction);
    }

    //  will wrap with div, because otherwise other wrappers will add and not remove classes after update
    public static Element wrapDiv(Element th) {
        return GPropertyTableBuilder.wrapSized(th, Document.get().createDivElement());
    }

    public final static GSize DEFAULT_HEADER_HEIGHT = GSize.CONST(34);

    // pretty similar to GGridPropertyTableBuilder.renderSized
    public static Element renderTD(Element th, boolean rerender, Boolean sortDir, String caption, String captionElementClass, AppBaseImage image, boolean tableToExcel, GPropertyDraw property, GSize gridUserHeight, GGridProperty grid) {
        GSize height = property != null ? property.getCaptionHeight() : null;
        if(height == null)
            height = gridUserHeight != null ? gridUserHeight : grid.getCaptionHeight(false);

        ImageHtmlOrTextType textType = property != null ? property.getCaptionHtmlOrTextType() : ImageHtmlOrTextType.OTHER;

        boolean hasDynamicImage = property != null && property.hasDynamicImage();
        boolean hasDynamicCaption = property != null && property.hasDynamicCaption();

        GFlexAlignment horzTextAlignment = property != null ? property.getCaptionAlignmentHorz() : GFlexAlignment.START;
        GFlexAlignment vertTextAlignment = property != null ? property.getCaptionAlignmentVert() : GFlexAlignment.CENTER;

        if(rerender) { // assert that property is the same (except order), so we don't clear (including clearFlexAlignment and clearTextAlignment) anything (however filling some props classes one more time, but it doesn't matter)
            GwtClientUtils.removeAllChildren(th);

            CellRenderer.clearRenderTextAlignment(th, horzTextAlignment, vertTextAlignment);

            FlexPanel.setGridHeight(th, null);

            GwtClientUtils.removeClassName(th, "caption-grid-header");
            GwtClientUtils.clearValueShrinkHorz(th, true, true);
        }

        boolean noImage = sortDir == null && image == null && !hasDynamicImage;
        boolean canBeHTML = !(caption != null && GwtClientUtils.containsHtmlTag(caption)); // && !hasDynamicCaption;
        boolean simpleText = noImage && canBeHTML;

        boolean isWrap = textType.isWrap();
        boolean wrapFixed = isWrap && textType.getWrapLines() != -1;

        boolean isTDorTH = GwtClientUtils.isTDorTH(th);
        boolean needWrap = isTDorTH && // have display:td
                        (!simpleText // we'll have to change the display
                        || isWrap && height != null // similar to TextBasedCellRenderer.canBeRenderedInTdCheck, height in td works as min-height
                        || wrapFixed); // we have to change the display
        if(needWrap) {
            th = wrapDiv(th);
            isTDorTH = false;
        }

        FlexPanel.setGridHeight(th, height);
        GwtClientUtils.addClassName(th, "caption-grid-header");

        Element renderElement = th;

        boolean renderedAlignment = false;
        if(isTDorTH) { //  || (simpleText && ((property != null && property.captionEllipsis) && !wrapFixed))) { also the problem is that vertical-align works only for table-cell and inline displays, which is not what we have, so we can't render text alignment for regular divs to provide ellipsis for example
            assert simpleText;
            renderTextAlignment(renderElement, horzTextAlignment, vertTextAlignment);
            renderedAlignment = true;
        }

        GwtClientUtils.renderValueShrinkHorz(th, true, true);

        // we'll render alignment with flex, and in all not simple text cases (will be wrap-img) we'll have to change the display
        if(!renderedAlignment && (!noImage || wrapFixed || (isWrap && !vertTextAlignment.equals(GFlexAlignment.START)))) // the last check is need to align start when the text doesn't fit
            th = wrapImageText(th);

        if (sortDir != null)
            th = wrapSortImg(th, sortDir);

        BaseImage.initImageText(th, textType);
        renderCaption(th, caption);
        renderCaptionElementClass(th, captionElementClass);
        renderImage(th, image);

        if(!renderedAlignment) {
            // we need this if text is wrapped or there are line breaks
            CellRenderer.renderWrapTextAlignment(th, horzTextAlignment, vertTextAlignment);

            CellRenderer.renderFlexAlignment(renderElement, horzTextAlignment, vertTextAlignment);
        }

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
        GwtClientUtils.addClassName(th, "wrap-text-not-empty");
        GwtClientUtils.addClassName(th, "wrap-img-horz");
        GwtClientUtils.addClassName(th, "wrap-img-start");

        Element img = (sortDir ? StaticImage.SORTUP : StaticImage.SORTDOWN).createImage();

        GwtClientUtils.addClassName(img, "wrap-text-img");
        th.appendChild(img);

        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        // extra classes
        GwtClientUtils.addClassName(img, "sort-img"); // needed for pivot hack
        GwtClientUtils.addClassName(wrappedTh, "sort-div"); // need to stretch if stretched

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