package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.util.Objects;

public class GIntervalValue implements Serializable {
    public long from;
    public long to;

    @SuppressWarnings("UnusedDeclaration")
    public GIntervalValue() {}

    public GIntervalValue(long from, long to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return from + "." + to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GIntervalValue)) return false;
        return from == (((GIntervalValue) o).from) && to == (((GIntervalValue) o).to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
