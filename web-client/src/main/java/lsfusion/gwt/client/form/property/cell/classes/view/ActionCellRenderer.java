package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.function.Consumer;

import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

// actually extends TextBasedCellRenderer for optimization purposes, when there are no images
public class ActionCellRenderer extends CellRenderer {

    private static final String ICON_EXECUTE = "action.png";

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

    private static void addImage(Element th, Consumer<ImageElement> imgProcessor) {
        ImageElement img = Document.get().createImageElement();
        img.addClassName("wrap-img-margins");
        imgProcessor.accept(img);

        th.insertFirst(img);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        element.addClassName("btn");
        element.addClassName("btn-secondary");
//        element.addClassName("btn-outline-light");

        element.setInnerText("..."); // need this to make getLastChild work
        JavaScriptObject node;
        if(hasImage(renderContext)) { // optimization;
            if(property.panelCaptionVertical)
                element.getStyle().setProperty("flexDirection", "column");
            addImage(element, imageElement -> { // assert that in renderStatic it is wrapped into wrap-center
                element.setPropertyObject(IMAGE, imageElement);
            });
        }
        node = element.getLastChild();

        element.setPropertyObject(TEXT, node);
        setLabelText(element, null);

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
    }

    public static void setImage(Element element, String absolutePath, boolean dynamicMargins) {
        ImageElement img = (ImageElement) element.getPropertyObject(IMAGE);
        if(dynamicMargins) {
            if(absolutePath.isEmpty()) {
                img.removeClassName("wrap-img-margins");
            } else {
                img.addClassName("wrap-img-margins");
            }
        }
        img.setSrc(absolutePath);
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
        if(hasImage(updateContext)) {
            String imagePath;
            boolean absolute = true;

            Object image;
            if(updateContext.isLoading() && property.isLoadingReplaceImage()) {
                imagePath = ICON_LOADING;
                absolute = false;
            } else if(property.hasDynamicImage()) {
                if ((image = updateContext.getImage()) instanceof String)
                    imagePath = GwtClientUtils.getAppDownloadURL((String) image, null, null);
                else
                    imagePath = "";
            } else if(property.hasStaticImage())
                imagePath = GwtClientUtils.getAppStaticImageURL(property.getImage().getUrl(enabled));
            else {
                imagePath = ICON_EXECUTE;
                absolute = false;
            }

            Consumer<String> setImage = absolutePath -> setImage(element, absolutePath, false);

            if(absolute)
                setImage.accept(imagePath);
            else
                GwtClientUtils.setThemeImage(imagePath, setImage);

            if(property.drawAsync) {
                element.setPropertyObject(ASYNCIMAGE, imagePath);
            }
        }

        element.setPropertyBoolean("disabled", !enabled);

        return false;
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "..." : "";
    }
}
