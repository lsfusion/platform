package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GDateTimeIntervalType extends GIntervalType {

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }

    @Override
    protected GADateType getTimeSeriesType() {
        return GDateTimeType.instance;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateTimeIntervalCaption();
    }
}
