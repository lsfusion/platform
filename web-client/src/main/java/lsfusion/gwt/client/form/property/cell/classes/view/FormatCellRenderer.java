package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public abstract class FormatCellRenderer<T> extends TextBasedCellRenderer {

    protected GFormatType getFormatType(RendererType rendererType) {
        return property.getFormatType(rendererType);
    }

    @Override
    public String format(PValue value, RendererType rendererType) {
        return getFormatType(rendererType).formatString(value, property.pattern);
    }

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
    }
}
