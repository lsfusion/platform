package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;

public class HTMLGridCellRenderer extends GridCellRenderer<Object> {
    public HTMLGridCellRenderer() { super(); }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        element.setInnerHTML("<iframe src=\"" + value + "\" style=\"width:100%; height:100%;\" >Unfortunately this content could not be displayed</iframe>");
    }

    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
}