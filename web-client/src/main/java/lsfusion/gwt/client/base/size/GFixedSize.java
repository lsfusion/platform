package lsfusion.gwt.client.base.size;

import lsfusion.gwt.client.base.GwtClientUtils;

import java.util.Objects;

public class GFixedSize extends GSimpleSize {

    public enum Type {
        EM, REM, PX;
    }

    public GFixedSize() {
    }

    private double value;
    private Type type;

    public GFixedSize(double value, Type type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public GSize scale(int count) {
        return new GFixedSize(value * count, type);
    }

    public static double convertFontSize = 12.0; // default font size used to convert usually from constants;
    public static GFixedSize getSize(double pixels, Type type) {
        return new GFixedSize(type == GFixedSize.Type.PX ? pixels : ((double)pixels / convertFontSize), type);
    }
    @Override
    protected double getPixelSize() {
        return type == GFixedSize.Type.PX ? value : value * convertFontSize;
    }

    @Override
    protected String getCalcString() {
        String typeString = null;
        switch (type) {
            case PX:
                typeString = "px";
                break;
            case REM:
                typeString = "rem";
                break;
            case EM:
                typeString = "em";
                break;
        }
        return value + typeString;
    }

    @Override
    public GSize div(int count) {
        return new GFixedSize(((double)value) / ((double) count), type);
    }

    @Override
    public GSize add(int px) {
        if(type == Type.PX)
            return new GFixedSize(value + px, type);

        return super.add(px);
    }

    @Override
    public Double getResizeSize() {
        return type == Type.PX || isZero() ? getPixelSize() : null;
    }

    @Override
    public GSize add(GSize size) {
        Type type;
        if((type = getCompatibleType(size)) != null)
            return new GFixedSize(value + ((GFixedSize) size).value, type);

        return super.add(size);
    }

    private Type getCompatibleType(GSize size) {
        if(!(size instanceof GFixedSize))
            return null;
        if((((GFixedSize) size).type == type))
            return type;
        if(isZero())
            return ((GFixedSize) size).type;
        if(size.isZero())
            return type;
        return null;
    }

    @Override
    public GSize subtract(GSize size) {
        Type type;
        if((type = getCompatibleType(size)) != null)
            return new GFixedSize(value - ((GFixedSize) size).value, type);

        return super.subtract(size);
    }

    @Override
    public GSize max(GSize size) {
        Type type;
        if((type = getCompatibleType(size)) != null)
            return new GFixedSize(Math.max(value, ((GFixedSize) size).value), type);

        return super.max(size);
    }

    @Override
    public GSize min(GSize size) {
        Type type;
        if((type = getCompatibleType(size)) != null)
            return new GFixedSize(Math.min(value, ((GFixedSize) size).value), type);

        return super.min(size);
    }

    @Override
    public boolean isZero() {
        return GwtClientUtils.equals(value, 0.0);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GFixedSize && GwtClientUtils.equals(((GFixedSize) o).value, value) &&
                type == ((GFixedSize) o).type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }
}
