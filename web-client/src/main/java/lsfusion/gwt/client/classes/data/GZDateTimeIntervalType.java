package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class GZDateTimeIntervalType extends GDateTimeIntervalType {

    public static GZDateTimeIntervalType instance = new GZDateTimeIntervalType();

    // we want to have string width independent of the timezone
    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return GDateTimeIntervalType.instance.getDefaultWidthString(propertyDraw);
    }

    @Override
    public String getIntervalType() {
        return "ZDATETIME";
    }

    @Override
    protected boolean isSingleLocal() {
        return false;
    }

    @Override
    protected GADateType getTimeSeriesType() {
        return GZDateTimeType.instance;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeZDateTimeIntervalCaption();
    }
}
