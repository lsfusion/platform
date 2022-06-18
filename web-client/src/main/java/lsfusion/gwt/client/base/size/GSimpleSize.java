package lsfusion.gwt.client.base.size;

public abstract class GSimpleSize extends GSize {

    public GSimpleSize() {
    }

    @Override
    public String getString() {
        return getCalcString();
    }

    @Override
    public String getOpCalcString() {
        return getCalcString();
    }
}
