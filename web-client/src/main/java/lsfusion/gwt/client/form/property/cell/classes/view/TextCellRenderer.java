package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class TextCellRenderer extends StringBasedCellRenderer {
    private final boolean rich;

    public TextCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    protected boolean isMultiLine() {
        return true;
    }

    @Override
    protected boolean isWordWrap() {
        return !rich;
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        if (rich) {
            element.setInnerHTML(EscapeUtils.sanitizeHtml(innerText));
        } else {
            super.setInnerContent(element, innerText);
        }
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }
}