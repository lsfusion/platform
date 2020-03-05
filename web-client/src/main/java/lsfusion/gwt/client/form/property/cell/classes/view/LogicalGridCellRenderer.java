package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class LogicalGridCellRenderer extends AbstractGridCellRenderer {
    private GPropertyDraw property;

    public LogicalGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            element.setAttribute("align", textAlignStyle.getCssName());
        }

        Style checkStyle;
        // logical class is rendered as checkbox input to make all checkboxes look the same.
        // in case of renderer we want to prevent checkbox from listening to mouse events.
        // for this purpose we use property "pointerEvents: none", which doesn't work in IE.
        if (GwtClientUtils.isIEUserAgent()) {
            ImageElement img = element.appendChild(Document.get().createImageElement());
            checkStyle = img.getStyle();
            checkStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
        } else {
            InputElement input = element.appendChild(Document.get().createCheckInputElement());
            input.setTabIndex(-1);
            checkStyle = input.getStyle();
            checkStyle.setProperty("pointerEvents", "none");
        }

        if (!isSingle) element.getStyle().setPosition(Style.Position.RELATIVE);
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        if (GwtClientUtils.isIEUserAgent()) {
            ImageElement img = element.getFirstChild().cast();
            img.setSrc(getCBImagePath(value));
        } else {
            InputElement input = element.getFirstChild().cast();
            input.setChecked(value != null && (Boolean) value);
        }
    }

    private String getCBImagePath(Object value) {
        boolean checked = value != null && (Boolean) value;
        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
    }
}
