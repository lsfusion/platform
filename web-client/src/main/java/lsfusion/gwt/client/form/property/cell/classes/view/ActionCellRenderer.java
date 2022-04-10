package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.function.Consumer;

import static lsfusion.gwt.client.base.GwtClientUtils.getDownloadURL;
import static lsfusion.gwt.client.base.GwtClientUtils.setThemeImage;
import static lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer.clearBasedTextFonts;
import static lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer.setBasedTextFonts;
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
    public boolean isSimpleText(RenderContext renderContext) {
        return !hasImage(renderContext);
    }
    @Override
    public boolean isSimpleText(UpdateContext updateContext) {
        return !hasImage(updateContext);
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
    protected Style.TextAlign getDefaultHorzTextAlignment() {
        return Style.TextAlign.CENTER;
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        element.addClassName("gwt-Button");

        String setText;
        Element textElement;
        if(!hasImage(renderContext)) { // optimization;
            textElement = element;
            setText = "...";
        } else {
            if(property.panelCaptionVertical)
                element.getStyle().setProperty("flexDirection", "column");
            textElement = GwtClientUtils.wrapAlignedFlexImg(element, imageElement -> { // assert that in renderStatic it is wrapped into wrap-center
                element.setPropertyObject(IMAGE, imageElement);
            });
            setText = null;
        }

        setPadding(element.getStyle());
        setBasedTextFonts(property, textElement, renderContext);

        element.setPropertyObject(TEXT, textElement);
        setLabelText(element, setText);

        // using widgets can lead to some leaks
        // also there is a problem with focuses (all inner elements, should be not focusable), outer borders and extra elements
//        AppImageButton button = new AppImageButton(property.imageHolder, null);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        element.removeClassName("gwt-Button");
        if(property.panelCaptionVertical)
            element.getStyle().clearProperty("flexDirection");
        element.getStyle().clearPadding();
        element.setPropertyObject(TEXT, null);

        if(!hasImage(renderContext))
            clearBasedTextFonts(property, element.getStyle(), renderContext);

        element.removeClassName("gwt-Button-disabled");
    }

    public static void setLabelText(Element element, String text) {
        ((Element)element.getPropertyObject(TEXT)).setInnerText(text != null ? text : "");
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

    private static void setPadding(Style style) {
        style.setPaddingTop(0, Style.Unit.PX);
        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingLeft(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingRight(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
    }

    @Override
    protected boolean renderedLoadingContent(UpdateContext updateContext) {
        return hasImage(updateContext) && property.isLoadingReplaceImage();
    }

    @Override
    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
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
                    imagePath = getDownloadURL((String) image, null, null, false);
                else
                    imagePath = "";
            } else if(property.hasStaticImage())
                imagePath = GwtClientUtils.getAppImagePath(property.getImage(enabled).url);
            else {
                imagePath = ICON_EXECUTE;
                absolute = false;
            }

            Consumer<String> setImage = absolutePath -> setImage(element, absolutePath, false);

            if(absolute)
                setImage.accept(imagePath);
            else
                setThemeImage(imagePath, setImage);

            if(property.drawAsync) {
                element.setPropertyObject(ASYNCIMAGE, imagePath);
            }
        }

        if(!enabled)
            element.addClassName("gwt-Button-disabled");
        else
            element.removeClassName("gwt-Button-disabled");

        return false;
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "..." : "";
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
