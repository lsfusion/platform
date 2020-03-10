package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class StringBasedGridCellRenderer<T> extends TextBasedGridCellRenderer<T> {
    StringBasedGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    protected void setInnerText(Element element, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                setInnerContent(element, REQUIRED_VALUE);
                element.addClassName("requiredValueString");
                element.removeClassName("nullValueString");
            } else {
                setInnerContent(element, EMPTY_VALUE);
                element.addClassName("nullValueString");
                element.removeClassName("requiredValueString");
            }
        } else {
            setInnerContent(element, innerText);
            element.removeClassName("nullValueString");
            element.removeClassName("requiredValueString");
        }
    }
}
