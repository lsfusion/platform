package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.view.AppImageButton;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.function.Consumer;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;
import static lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer.setBasedTextFonts;
import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

// actually extends TextBasedCellRenderer for optimization purposes, when there are no images
public class ActionCellRenderer extends CellRenderer {

    private final GPropertyDraw property;

    public ActionCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public static final String TEXT = "lsf-text-button";
    public static final String IMAGE = "lsf-image-button";

    @Override
    public void renderStatic(Element element, RenderContext renderContext) {
        element.addClassName("gwt-Button");

        Style style = element.getStyle();
        setPadding(style);

        String setText;
        Element textElement;
        if(property.imageHolder == null) { // optimization;
            Style.TextAlign textAlignStyle = property.getTextAlignStyle();
            if (textAlignStyle == null)
                textAlignStyle = Style.TextAlign.CENTER;
            style.setTextAlign(textAlignStyle);

            textElement = element;
            setText = "...";
        } else {
            textElement = GwtClientUtils.wrapCenteredImg(element, null, imageElement -> {
                element.setPropertyObject(IMAGE, imageElement);
            });
            setText = null;
        }
        element.setPropertyObject(TEXT, textElement);
        setLabelText(element, setText);

        // using widgets can lead to some leaks
        // also there is a problem with focuses (all inner elements, should be not focusable), outer borders and extra elements
//        AppImageButton button = new AppImageButton(property.imageHolder, null);
    }

    public static void setLabelText(Element element, String text) {
        ((Element)element.getPropertyObject(TEXT)).setInnerText(text != null ? text : "");
    }

    public static void setImage(Element element, String absolutePath, Consumer<String> prevImage) {
        ImageElement img = (ImageElement) element.getPropertyObject(IMAGE);
        if(prevImage != null)
            prevImage.accept(img.getSrc());
        img.setSrc(absolutePath);
    }

    @Override
    public int getWidthPadding() {
        return BUTTON_HORIZONTAL_PADDING;
    }
    @Override
    public int getHeightPadding() {
        return 0;
    }

    private static void setPadding(Style style) {
        style.setPaddingTop(0, Style.Unit.PX);
        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingLeft(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingRight(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
    }

    @Override
    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        boolean enabled = (value != null) && (Boolean) value;

        if(property.imageHolder != null) {
            ImageDescription image = property.getImage(enabled);
            setImage(element, image != null ? GwtClientUtils.getAppImagePath(image.url) : null, null);
        }
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "..." : "";
    }
}
