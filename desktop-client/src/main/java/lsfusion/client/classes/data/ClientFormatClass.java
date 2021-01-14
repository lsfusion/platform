package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.text.Format;

public abstract class ClientFormatClass<F extends Format> extends ClientDataClass {

    public abstract Format getDefaultFormat();

    public abstract F createUserFormat(String pattern);

    protected F getEditFormat(ClientPropertyDraw propertyDraw) {
        return getEditFormat(propertyDraw.getFormat(), false);
    }

    protected F getEditFormat(ClientPropertyDraw propertyDraw, boolean edit) {
        return getEditFormat(propertyDraw.getFormat(), edit);
    }

    @Override
    protected String getDefaultWidthString(ClientPropertyDraw propertyDraw) {
        Object defaultWidthValue = getDefaultWidthValue();
        if(defaultWidthValue != null)
            return getEditFormat(propertyDraw, true).format(defaultWidthValue);
        return super.getDefaultWidthString(propertyDraw);
    }

    protected Object getDefaultWidthValue() {
        return null;
    }

    protected F getEditFormat(Format format, boolean width) {
        return (F) format;
    }
}
