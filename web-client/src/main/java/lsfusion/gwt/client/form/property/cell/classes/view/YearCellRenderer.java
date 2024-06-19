package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public class YearCellRenderer extends IntegralCellRenderer {
    public YearCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }
}
