package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

// actually extends TextBasedCellRenderer for optimization purposes, when there are no images
public class ActionCellRenderer extends CellRenderer {

    public ActionCellRenderer(GPropertyDraw property) {
        super(property);
    }

    public static final String TEXT = "lsf-text-button";
    public static final String IMAGE = "lsf-image-button";
    public static final String ASYNCIMAGE = "lsf-async-image";

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
        element.addClassName("btn");

        element.setInnerText("..."); // need this to make getLastChild work
        JavaScriptObject node;
        if(hasImage(renderContext)) { // optimization;
            Element img;
            if(property.hasDynamicImage()) // app download image
                img = GwtClientUtils.createAppDownloadImage(null, null);
            else if(property.hasStaticImage()) // app static image
                img = property.appStaticImage.createImage();
            else // static image
                img = StaticImage.EXECUTE.createImage();

            if(property.panelCaptionVertical) {
                element.getStyle().setProperty("flexDirection", "column");
                img.addClassName("wrap-img-vert-margins");
            } else
                img.addClassName("wrap-img-horz-margins");

            element.setPropertyObject(IMAGE, img);
            element.insertFirst(img);
        }
        node = element.getLastChild();

        element.setPropertyObject(TEXT, node);
        setLabelText(element, null); // to remove "..."

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
        element.removeClassName("btn");

        if(hasImage(renderContext)) {
            if (property.panelCaptionVertical) {
                element.getStyle().clearProperty("flexDirection");
            }
        }
//        element.getStyle().clearPadding();

        element.setPropertyObject(TEXT, null);

        clearBasedTextFonts(property, element, renderContext);
//        if(GwtClientUtils.isTDorTH(element)) { // otherwise we'll use flex alignment (however text alignment would also do)
//            clearRenderTextAlignment(element);
//            return true;
//            }
        return false;
    }

    public static void setLabelText(Element element, String text) {
        ((Node)element.getPropertyObject(TEXT)).setNodeValue(text != null ? text : "");

        if(element.getPropertyObject(IMAGE) != null) { // optimization
            if (text != null && !text.equals("")) {
                element.addClassName("wrap-text-not-empty");
            } else {
                element.removeClassName("wrap-text-not-empty");
            }
        }
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
    public boolean updateContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        boolean enabled = !property.isReadOnly() && (value != null) && (Boolean) value;

        // we have it here and not in renderStaticContent because of using enabled
        Element imageElement = (Element) element.getPropertyObject(IMAGE);
        if(imageElement != null) {
            assert hasImage(updateContext);

            StaticImage overrideImage = updateContext.isLoading() && property.isLoadingReplaceImage() ? StaticImage.LOADING_IMAGE_PATH : null;
            if(property.hasDynamicImage()) // app download image
                GwtClientUtils.setAppDownloadImageSrc(imageElement, updateContext.getImage(), null, overrideImage);
            else if(property.hasStaticImage()) // app static image
                property.appStaticImage.setImageSrc(imageElement, enabled, overrideImage);
            else // static image
                StaticImage.EXECUTE.setImageSrc(imageElement, overrideImage);
        }

        element.setPropertyBoolean("disabled", !enabled);

        return false;
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "..." : "";
    }
}
