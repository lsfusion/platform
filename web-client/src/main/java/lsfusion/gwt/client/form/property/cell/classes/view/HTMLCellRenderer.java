package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class HTMLCellRenderer extends CellRenderer<Object> {
    public HTMLCellRenderer() { super(); }

    @Override
    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        element.setInnerHTML("<iframe src=\"" + value + "\" style=\"width:100%; height:100%;\" >Unfortunately this content could not be displayed</iframe>");
    }

    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
}