package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LinkCellRenderer extends StringBasedCellRenderer {

    public LinkCellRenderer(GPropertyDraw property) {
        super(property, false);
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        String url = PValue.getStringValue(value);
        if(url != null) {
            element.setAttribute("href", url);
        } else {
            element.removeAttribute("href");
        }
        return super.updateContent(element, value, extraValue, updateContext);
    }
}
