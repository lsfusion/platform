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

    @Override
    protected String getRequiredStringValue(Element element) {
        element.removeClassName("nullValueString");
        return super.getRequiredStringValue(element);
    }

    @Override
    protected String getNullStringValue(Element element) {
        element.addClassName("nullValueString");
        return MainFrame.showNotDefinedStrings ? NOT_DEFINED_VALUE : super.getNullStringValue(element);
    }

    @Override
    protected String getNotNullStringValue(String innerText, Element element) {
        boolean isEmptyValueString = false;
        if(innerText.isEmpty()) {
            if (MainFrame.showNotDefinedStrings)
                innerText = EscapeUtils.UNICODE_NBSP;
            else {
                isEmptyValueString = true;
                innerText = EMPTY_VALUE;
            }
        }
        if(isEmptyValueString)
            element.addClassName("nullValueString");
        else
            element.removeClassName("nullValueString");
        return super.getNotNullStringValue(innerText, element);
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        // it's tricky, since we use simpleText and thus put everything in td
        // so we cannot use max-height for cutting extra text (td doesn't respect it), so we'll have to set NOWRAP whitespace
        if(!isMultiLine())
            element.getStyle().setWhiteSpace(innerText.contains("\n") ? Style.WhiteSpace.NOWRAP : Style.WhiteSpace.PRE);

        super.setInnerContent(element, innerText);
    }
}
