package lsfusion.interop.form.property.cell;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class IntervalValue implements Serializable {
    public final long from;
    public final long to;

    public IntervalValue(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public static IntervalValue parseIntervalValue(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof IntervalValue)
            return (IntervalValue) obj;
        String str = String.valueOf(obj);
        int indexOfDecimal = str.indexOf(".");
        Long from = Long.parseLong(indexOfDecimal < 0 ? str : str.substring(0, indexOfDecimal));
        Long to = Long.parseLong(indexOfDecimal < 0 ? str : str.substring(indexOfDecimal + 1));
        return new IntervalValue(from, to);
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(toString());
    }

    @Override
    public String toString() {
        return from + "." + to;
    }

    //needs equals and hashCode because this class is used inside the Map key in ClientPropertyData.getFieldValue fieldData.get(...)
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IntervalValue that = (IntervalValue) o;
        return from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
