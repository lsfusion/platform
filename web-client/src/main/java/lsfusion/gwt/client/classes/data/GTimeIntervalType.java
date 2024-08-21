package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GTimeIntervalType extends GIntervalType {

    public static GTimeIntervalType instance = new GTimeIntervalType();

    @Override
    public String getIntervalType() {
        return "TIME";
    }

    @Override
    protected GADateType getTimeSeriesType() {
        return GTimeType.instance;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTimeIntervalCaption();
    }
}
