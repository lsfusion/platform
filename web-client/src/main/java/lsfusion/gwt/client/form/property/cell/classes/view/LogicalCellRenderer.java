package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LogicalCellRenderer extends CellRenderer {
    private GPropertyDraw property;

    public LogicalCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderStatic(Element element, RenderContext renderContext) {
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
            DivElement checkWrapper = element.appendChild(Document.get().createDivElement());
            checkWrapper.addClassName("logicalRendererWrapper");
            
            InputElement input = checkWrapper.appendChild(Document.get().createCheckInputElement());
            input.addClassName("logicalRendererCheckBox");
        }
    }

    @Override
    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        if (GwtClientUtils.isIEUserAgent()) {
            ImageElement img = element.getFirstChild().cast();
            img.setSrc(getCBImagePath(value));
        } else {
            InputElement input = element.getFirstChild().getFirstChild().cast();
            input.setTabIndex(-1);
            input.setChecked(value != null && (Boolean) value);
        }
    }

    private String getCBImagePath(Object value) {
        boolean checked = value != null && (Boolean) value;
        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "TRUE" : "FALSE";
    }
}
