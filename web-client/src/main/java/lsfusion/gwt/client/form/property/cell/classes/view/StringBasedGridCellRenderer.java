package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class StringBasedGridCellRenderer<T> extends TextBasedGridCellRenderer<T> {
    StringBasedGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    protected void setInnerText(Element div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
                div.removeClassName("nullValueString");
            } else {
                div.setInnerText(EMPTY_VALUE);
                div.addClassName("nullValueString");
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("nullValueString");
            div.removeClassName("requiredValueString");
        }
    }

}
