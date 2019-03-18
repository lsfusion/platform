package lsfusion.client.classes;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.text.Format;

public abstract class ClientFormatClass<F extends Format> extends ClientDataClass {

    public abstract Format getDefaultFormat();

    public abstract F createUserFormat(String pattern);

    protected F getEditFormat(ClientPropertyDraw propertyDraw) {
        return getEditFormat(propertyDraw.getFormat());
    }

    @Override
    protected String getDefaultWidthString(ClientPropertyDraw propertyDraw) {
        Object defaultWidthValue = getDefaultWidthValue();
        if(defaultWidthValue != null)
            return getEditFormat(propertyDraw).format(defaultWidthValue);
        return super.getDefaultWidthString(propertyDraw);
    }

    protected Object getDefaultWidthValue() {
        return null;
    }

    protected F getEditFormat(Format format) {
        return (F) format;
    }
}
