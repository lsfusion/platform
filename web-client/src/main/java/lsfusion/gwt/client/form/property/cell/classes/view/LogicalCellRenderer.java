package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LogicalCellRenderer extends CellRenderer {

    public LogicalCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected Style.TextAlign getDefaultHorzAlignment() {
        return Style.TextAlign.CENTER;
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
//        if (GwtClientUtils.isIEUserAgent()) {
//            ImageElement img = element.appendChild(Document.get().createImageElement());
//            checkStyle = img.getStyle();
//            checkStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
//        }
        InputElement input = Document.get().createCheckInputElement();
        input.addClassName("logicalRendererCheckBox");
        element.appendChild(input);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
    }

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
//        if (GwtClientUtils.isIEUserAgent()) {
//            ImageElement img = element.getFirstChild().cast();
//            img.setSrc(getCBImagePath(value));
//        } else {
        InputElement input = element.getFirstChild().cast();
        input.setTabIndex(-1);
        input.setChecked(value != null && (Boolean) value);
//        }
    }

//    private String getCBImagePath(Object value) {
//        boolean checked = value != null && (Boolean) value;
//        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
//    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "TRUE" : "FALSE";
    }
}
