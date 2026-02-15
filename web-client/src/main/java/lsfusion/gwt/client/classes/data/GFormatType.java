package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.classes.GTextBasedType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public abstract class GFormatType extends GTextBasedType {

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        PValue defaultWidthValue = getDefaultWidthValue();
        if(defaultWidthValue != null)
            return formatString(defaultWidthValue, propertyDraw.getPattern());
        return super.getDefaultWidthString(propertyDraw);
    }

    // Override formatString from GType to provide formatted string representation
    @Override
    public abstract String formatString(PValue value, String pattern);

    protected PValue getDefaultWidthValue() {
        return null;
    }

    public String formatISOString(PValue value) {
        throw new UnsupportedOperationException();
    }
}
