package lsfusion.gwt.client.base.size;

public abstract class GCalcSize extends GSize {

    public GCalcSize() {
    }

    @Override
    public String getString() {
        return "calc(" + getCalcString() + ")";
    }

    @Override
    public String getOpCalcString() {
        return "(" + getCalcString() + ")";
    }
}
