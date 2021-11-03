package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class HTMLTextCellRenderer extends StringBasedCellRenderer {

    public HTMLTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        element.setInnerHTML("<div>"+ innerText +"</div>");
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }
}
