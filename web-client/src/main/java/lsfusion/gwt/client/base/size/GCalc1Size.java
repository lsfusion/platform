package lsfusion.gwt.client.base.size;

import java.util.Objects;

public class GCalc1Size extends GCalcSize {

    public enum Type {
        SCALE, DIV
    }
    private GSize size;
    private Type type;
    private int scale;

    public GCalc1Size() {
    }

    public GCalc1Size(GSize size, Type type, int scale) {
        this.size = size;
        this.type = type;
        this.scale = scale;
    }

    @Override
    protected double getPixelSize() {
        switch (type) {
            case DIV:
                return size.getPixelSize() / scale;
            case SCALE:
                return size.getPixelSize() * scale;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getCalcString() {
        switch (type) {
            case DIV:
                return size.getOpCalcString() + " / " + scale;
            case SCALE:
                return size.getOpCalcString() + " * " + scale;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GCalc1Size && size.equals(((GCalc1Size) o).size) &&
                type == ((GCalc1Size) o).type && scale == ((GCalc1Size) o).scale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, type, scale);
    }
}
