package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.TypeInputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class ColorCellRenderer extends TextBasedCellRenderer {

    public ColorCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getBackground(UpdateContext updateContext) {
        if(!isTagInput()) {
            String colorValue = getColorValue(updateContext.getValue());
            if (colorValue != null)
                return colorValue;
        }

        return super.getBackground(updateContext);
    }

    private String getColorValue(PValue value) {
        return PValue.getColorStringValue(value);
    }

    @Override
    public String format(PValue value, RendererType rendererType) {
        return getColorValue(value);
    }
}
