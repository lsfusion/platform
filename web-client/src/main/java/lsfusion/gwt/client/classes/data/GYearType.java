package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.NumberFormat;

public class GYearType extends GIntegerType {
    public static GYearType instance = new GYearType();

    @Override
    protected NumberFormat getDefaultFormat() {
        return NumberFormat.getFormat("0");
    }
}
