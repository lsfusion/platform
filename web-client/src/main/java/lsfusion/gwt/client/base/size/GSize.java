package lsfusion.gwt.client.base.size;

import java.io.Serializable;

public abstract class GSize implements Serializable {

    public GSize() {
    }

    public static GSize CONST(int px) {
        return getSize(px, COMPONENT_TYPE);
    }

    private static GFixedSize.Type COMPONENT_TYPE = GFixedSize.Type.REM;
    public static GFixedSize.Type VALUE_TYPE = GFixedSize.Type.EM;
    private static GFixedSize.Type SIZE_TYPE = GFixedSize.Type.PX;

    public static GSize getImageSize(int pixels) {
        return getSize(pixels, SIZE_TYPE);
    }

    // everything about explicit components sizing
    public static GSize getComponentSize(int pixels) {
        return getSize(pixels, COMPONENT_TYPE);
    }
    public static GSize getContainerNSize(Integer pixels) {
        return getNSize(pixels, COMPONENT_TYPE);
    }
    public static GSize getValueSize(int pixels) {
        return getSize(pixels, VALUE_TYPE);
    }
    public static GSize getCalcValueSize(int pixels) { // "font dependent" pixels (not explicit constants like in getValueSize)
        return getValueSize(pixels);
    }

    // used for flexing values with the same value as size
    public double getValueFlexSize() {
        return getPixelSize();
    }

    // everyting with calculating / reading actual size - fixing / preferred (window)
    public static GSize getOffsetSize(int pixels) {
        return getSize(pixels, SIZE_TYPE);
    }
    // everything with resizing components / grids
    public static GSize getResizeNSize(Integer pixels) {
        return getNSize(pixels, SIZE_TYPE);
    }
    public static GSize getResizeSize(int pixels) {
        return getSize(pixels, SIZE_TYPE);
    }
    public static GSize getResizeSize(double pixels) {
        return getSize(pixels, SIZE_TYPE);
    }
    public Integer getIntResizeSize() {
        Double size = getResizeSize();
        if(size != null)
            return (int) Math.round(size);
        return null;
    }
    public Double getResizeSize() {
        return null;
    }

    // pivots
    public int getPivotSize() {
        return (int) Math.round(getPixelSize());
    }

    private static GSize getNSize(Integer pixels, GFixedSize.Type type) {
        if(pixels == null)
            return null;
        return getSize(pixels, type);
    }
    private static GSize getSize(int pixels, GFixedSize.Type type) {
        return getSize((double)pixels, type);
    }
    private static GSize getSize(double pixels, GFixedSize.Type type) {
        return GFixedSize.getSize(pixels, type);
    }
    protected abstract double getPixelSize();

    public abstract String getString();

    public abstract String getOpCalcString();

    protected abstract String getCalcString();

    public boolean isZero() {
        return false;
    }

    public final static GSize ZERO = CONST(0);

    public GSize scale(int count) {
        return new GCalc1Size(this, GCalc1Size.Type.SCALE, count);
    }
    public GSize div(int count) {
        return new GCalc1Size(this, GCalc1Size.Type.DIV, count);
    }

    // grid scrollbars and margins paddings for not auto sized components
    // scrollbars can go away, after the sticky refactoring, others, after changing to rendering with "auto size", reading size (and memoizing all that)
    public GSize add(int px) {
        return add(getSize(px, COMPONENT_TYPE));
    }

    // actually is used only for grid layouting (grid sticky)
    public GSize add(GSize size) {
        return new GCalc2Size(this, size, GCalc2Size.Type.ADD);
    }
    public GSize subtract(GSize size) {
        return new GCalc2Size(this, size, GCalc2Size.Type.SUBTRACT);
    }

    public GSize max(GSize size) {
        return new GCalc2Size(this, size, GCalc2Size.Type.MAX);
    }
    public GSize min(GSize size) {
        return new GCalc2Size(this, size, GCalc2Size.Type.MIN);
    }
}
