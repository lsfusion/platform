package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.MainFrame;

public abstract class StringBasedGridCellRenderer<T> extends TextBasedGridCellRenderer<T> {
    StringBasedGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    protected void setInnerText(Element element, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                setInnerContent(element, getRequiredStringValue());
                element.addClassName("requiredValueString");
                element.removeClassName("nullValueString");
            } else {
                setInnerContent(element, MainFrame.showNotDefinedStrings ? NOT_DEFINED_VALUE : "");
                element.addClassName("nullValueString");
                element.removeClassName("requiredValueString");
            }
        } else {
            boolean empty = innerText.isEmpty() && !MainFrame.showNotDefinedStrings;
            setInnerContent(element, empty ? EMPTY_VALUE : innerText);
            if(empty)
                element.addClassName("nullValueString");
            else
                element.removeClassName("nullValueString");
            element.removeClassName("requiredValueString");
        }
    }

    protected void setInnerContent(Element element, String innerText) {
        element.setInnerText(innerText);
    }
}
