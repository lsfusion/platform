package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.MainFrame;

public abstract class StringBasedCellRenderer<T> extends TextBasedCellRenderer<T> {
    StringBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    protected void setInnerText(Element element, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                setInnerHTML(element, getRequiredStringValue());
                element.addClassName("requiredValueString");
                element.removeClassName("nullValueString");
            } else {
                setInnerContent(element, MainFrame.showNotDefinedStrings ? NOT_DEFINED_VALUE : EscapeUtils.UNICODE_NBSP);
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
        // it's tricky, since we use simpleText and thus put everything in td
        // so we cannot use max-height for cutting extra text (td doesn't respect it), so we'll have to set NOWRAP whitespace
        if(!isMultiLine())
            element.getStyle().setWhiteSpace(innerText.contains("\n") ? Style.WhiteSpace.NOWRAP : Style.WhiteSpace.PRE);

        element.setInnerText(innerText);
    }

    protected void setInnerHTML(Element element, String innerHTML) {
        element.setInnerHTML(innerHTML);
    }
}
