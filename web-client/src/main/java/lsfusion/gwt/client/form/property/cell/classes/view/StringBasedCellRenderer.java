package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class StringBasedCellRenderer<T> extends SimpleTextBasedCellRenderer<T> {
    protected StringBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected boolean setInnerContent(Element element, String innerText) {
        // it's tricky, since we use simpleText and thus put everything in td
        // so we cannot use max-height for cutting extra text (td doesn't respect it), so we'll have to set NOWRAP whitespace
        if(!isMultiLine())
            element.getStyle().setWhiteSpace(innerText.contains("\n") ? Style.WhiteSpace.NOWRAP : Style.WhiteSpace.PRE);

        return super.setInnerContent(element, innerText);
    }
}
