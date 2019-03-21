package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class NumberGridCellRenderer extends FormatGridCellRenderer<Number, NumberFormat> {

    public NumberGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String renderToString(Number value) {
        return format.format(value);
    }

    @Override
    protected void setInnerText(DivElement div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
            } else {
                div.setInnerText(EscapeUtils.UNICODE_NBSP);
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("requiredValueString");
        }
    }
}
