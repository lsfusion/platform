package lsfusion.gwt.client.classes.data;

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
}
