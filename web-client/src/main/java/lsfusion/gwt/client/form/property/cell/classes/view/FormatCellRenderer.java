package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatCellRenderer<T, F> extends TextBasedCellRenderer<T> {
    protected F format;

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
        updateFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }

    @Override
    protected void setInnerText(Element element, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                element.setInnerHTML(getRequiredStringValue());
                element.addClassName("requiredValueString");
            } else {
                element.setInnerText(EscapeUtils.UNICODE_NBSP);
                element.removeClassName("requiredValueString");
            }
        } else {
            element.setInnerText(innerText);
            element.removeClassName("requiredValueString");
        }
    }
}
