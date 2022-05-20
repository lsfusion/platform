package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class GFormatType extends GDataType {

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        Object defaultWidthValue = getDefaultWidthValue();
        if(defaultWidthValue != null)
            return formatString(defaultWidthValue, propertyDraw.pattern);
        return super.getDefaultWidthString(propertyDraw);
    }

    public abstract String formatString(Object value, String pattern);

    protected Object getDefaultWidthValue() {
        return null;
    }
}
