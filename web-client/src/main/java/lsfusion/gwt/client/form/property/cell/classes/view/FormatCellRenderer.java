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
    public String format(PValue value, RendererType rendererType, String pattern) {
        // input type number does not support formatting(e.g., spaces).
        if(rendererType == RendererType.CELL && isTagInput() && getInputType(rendererType).inputType.isNumber())
            return PValue.getStringValue(value);

        return getFormatType(rendererType).formatString(value, pattern);
    }

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
    }
}
