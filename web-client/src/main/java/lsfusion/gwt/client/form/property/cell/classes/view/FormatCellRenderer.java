package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.GFullInputType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public abstract class FormatCellRenderer<T> extends TextBasedCellRenderer {

    protected GFormatType getFormatType(RendererType rendererType) {
        return property.getFormatType(rendererType);
    }

    protected boolean isNative(GFullInputType fullInputType) {
        return false;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        GFormatType formatType = getFormatType(rendererType);
        return rendererType == RendererType.CELL && isTagInput() && isNative(getInputType(rendererType)) ?
                formatType.formatISOString(value) : formatType.formatString(value, pattern);
    }

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
    }
}
