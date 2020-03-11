package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatGridCellRenderer<T, F> extends TextBasedGridCellRenderer<T> {
    protected F format;

    public FormatGridCellRenderer(GPropertyDraw property) {
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
                element.setInnerText(REQUIRED_VALUE);
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
