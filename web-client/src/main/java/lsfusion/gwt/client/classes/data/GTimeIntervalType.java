package lsfusion.gwt.client.classes.data;

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
}
