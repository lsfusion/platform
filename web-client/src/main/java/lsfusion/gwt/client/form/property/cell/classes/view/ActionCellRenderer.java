package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

// actually extends TextBasedCellRenderer for optimization purposes, when there are no images
public class ActionCellRenderer extends CellRenderer {

    public ActionCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean canBeRenderedInTD() {
        return false; // since for now we want button element
    }

    private boolean hasImage(boolean globalCaptionIsDrawn) {
        return globalCaptionIsDrawn || property.hasStaticImage() || property.hasDynamicImage();
    }

    protected boolean hasImage(RenderContext renderContext) {
        return hasImage(renderContext.globalCaptionIsDrawn());
    }

    protected boolean hasImage(UpdateContext updateContext) {
        return hasImage(updateContext.globalCaptionIsDrawn());
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        if(property.hasChangeAction)
            element.addClassName("btn");

        BaseImage.initImageText(element);

        setBasedTextFonts(property, element, renderContext);
        // we can't use text alignment for several reasons:
        // button does not support vertical-align
        // vertical-align doesn't work properly with images (it works really odd, and has to be aligned manually with margins)
//        if(GwtClientUtils.isTDorTH(element)) { // otherwise we'll use flex alignment (however text alignment would also do)
//            renderTextAlignment(property, element);
//            return true;
//        }
//        }
        return false;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        if(property.hasChangeAction)
            element.removeClassName("btn");

        BaseImage.clearImageText(element, property.panelCaptionVertical);

        clearBasedTextFonts(property, element, renderContext);
//        if(GwtClientUtils.isTDorTH(element)) { // otherwise we'll use flex alignment (however text alignment would also do)
//            clearRenderTextAlignment(element);
//            return true;
//            }
        return false;
    }

    @Override
    public int getWidthPadding() {
        return BUTTON_HORIZONTAL_PADDING;
    }

    @Override
    protected boolean renderedLoadingContent(UpdateContext updateContext) {
        return hasImage(updateContext) && property.isLoadingReplaceImage();
    }
    @Override
    protected BaseImage getExtraValue(UpdateContext updateContext) {
        if(hasImage(updateContext)) {
            if(updateContext.isLoading() && property.isLoadingReplaceImage())
                return StaticImage.LOADING_IMAGE_PATH;
            else if(property.hasDynamicImage())
                return updateContext.getImage(); // was converted in convertFileValue
            else if(property.hasStaticImage())
                return property.appImage;
            else
                return StaticImage.EXECUTE;
        }
        return null;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        if(extraValue != null)
            BaseImage.updateImage((BaseImage) extraValue, element, property.panelCaptionVertical);

        boolean enabled = !property.isReadOnly() && getActionValue(value);
        element.setPropertyBoolean("disabled", !enabled);

        return false;
    }

    @Override
    public String format(PValue value) {
        return getActionValue(value) ? "..." : "";
    }

    private boolean getActionValue(PValue value) {
        return PValue.getBooleanValue(value);
    }
}
