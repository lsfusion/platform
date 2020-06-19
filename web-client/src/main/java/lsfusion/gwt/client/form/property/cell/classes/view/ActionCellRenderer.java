package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.view.AppImageButton;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;
import static lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer.setBasedTextFonts;
import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

public class ActionCellRenderer extends CellRenderer {

    private final GPropertyDraw property;

    public ActionCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public static final String WIDGET = "widget";

    @Override
    public void renderStatic(Element element, RenderContext renderContext) {
        element.addClassName("gwt-Button");

        // using widgets can lead to some leaks
        AppImageButton button = new AppImageButton(property.imageHolder, null);

        Style style = element.getStyle();

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            style.setTextAlign(textAlignStyle);
        }

        button.setText(property.getImage() != null ? null : "...");

        element.appendChild(button.getElement());
        element.setPropertyObject(WIDGET, button);
    }

//    public static void setPadding(Style style) {
//        style.setPaddingTop(0, Style.Unit.PX);
//        style.setPaddingBottom(0, Style.Unit.PX);
//        style.setPaddingLeft(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
//        style.setPaddingRight(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
//    }

    @Override
    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        AppImageButton button = (AppImageButton) element.getPropertyObject(WIDGET);

        boolean enabled = (value != null) && (Boolean) value;

        ImageDescription image = property.getImage(enabled);
        button.setAppImagePath(image != null ? image.url : null);
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "..." : "";
    }
}
