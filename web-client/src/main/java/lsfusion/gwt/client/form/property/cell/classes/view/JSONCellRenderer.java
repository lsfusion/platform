package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public class JSONCellRenderer extends TextCellRenderer {

    public JSONCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        String stringValue = super.format(value, rendererType, pattern);
        return formatJSON(stringValue);
    }

    private static native String formatJSON(String value)/*-{
        if (value == null || value === '')
            return value;
        try {
            return JSON.stringify(JSON.parse(value), null, 2);
        } catch (e) {
            return value;
        }
    }-*/;
}
