package lsfusion.interop.form.property.cell;

import java.io.Serializable;
import java.math.BigDecimal;

public class IntervalValue implements Serializable {
    public long from;
    public long to;

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
}
