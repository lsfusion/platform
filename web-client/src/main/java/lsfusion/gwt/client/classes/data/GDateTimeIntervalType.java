package lsfusion.gwt.client.classes.data;

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
}
