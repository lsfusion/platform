package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.Date;

public abstract class GFormatType<F> extends GDataType {

    public abstract F getFormat(String pattern);

    public F getEditFormat(GPropertyDraw propertyDraw) {
        return getFormat(propertyDraw.pattern);
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        Object defaultWidthValue = getDefaultWidthValue();
        if(defaultWidthValue != null)
            return ((DateTimeFormat)getEditFormat(propertyDraw)).format((Date) defaultWidthValue);
        return super.getDefaultWidthString(propertyDraw);
    }

    protected Object getDefaultWidthValue() {
        return null;
    }
}
