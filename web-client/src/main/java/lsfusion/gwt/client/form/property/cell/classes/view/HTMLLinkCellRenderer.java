package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public class HTMLLinkCellRenderer extends HTMLBasedCellRenderer {
    public HTMLLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getContentHTMLValue(PValue value) {
        return "<iframe src=\"" + super.getContentHTMLValue(value) + "\" style=\"width:100%; height:100%;\" >Unfortunately this content could not be displayed</iframe>";
    }
}