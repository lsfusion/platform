package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GDateIntervalType extends GIntervalType{

    public static GDateIntervalType instance = new GDateIntervalType();

    @Override
    public String getIntervalType() {
        return "DATE";
    }

    @Override
    protected GADateType getTimeSeriesType() {
        return GDateType.instance;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateIntervalCaption();
    }
}
