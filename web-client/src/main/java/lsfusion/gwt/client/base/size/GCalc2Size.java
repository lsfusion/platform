package lsfusion.gwt.client.base.size;

import java.util.Objects;

public class GCalc2Size extends GCalcSize {

    public enum Type {
        ADD, SUBTRACT, MAX, MIN
    }
    private GSize size1;
    private GSize size2;
    private Type type;

    public GCalc2Size() {
    }

    public GCalc2Size(GSize size1, GSize size2, Type type) {
        this.size1 = size1;
        this.size2 = size2;
        this.type = type;
    }

    @Override
    protected double getPixelSize() {
        switch (type) {
            case ADD:
                return size1.getPixelSize() + size2.getPixelSize();
            case SUBTRACT:
                return size1.getPixelSize() - size2.getPixelSize();
            case MAX:
                return Math.max(size1.getPixelSize(), size2.getPixelSize());
            case MIN:
                return Math.min(size1.getPixelSize(), size2.getPixelSize());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getCalcString() {
        switch (type) {
            case ADD:
                return size1.getOpCalcString() + " + " + size2.getOpCalcString();
            case SUBTRACT:
                return size1.getOpCalcString() + " - " + size2.getOpCalcString();
            case MAX:
                return "max(" + size1.getCalcString() + "," + size2.getCalcString() + ")";
            case MIN:
                return "min(" + size1.getCalcString() + "," + size2.getCalcString() + ")";
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GCalc2Size && size1.equals(((GCalc2Size) o).size1) &&
                size2.equals(((GCalc2Size) o).size2) &&
                type == ((GCalc2Size) o).type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size1, size2, type);
    }
}
