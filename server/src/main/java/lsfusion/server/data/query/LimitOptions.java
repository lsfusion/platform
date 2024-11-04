package lsfusion.server.data.query;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.form.stat.LimitOffset;

public class LimitOptions {

    public final static LimitOptions HASLIMITDISTINCTVALUES = new LimitOptions();
    public final static LimitOptions HASLIMIT = new LimitOptions();
    public final static LimitOptions HASLIMITOFFSET = new LimitOptions();
    public final static LimitOptions NOLIMIT = new LimitOptions();

    public static LimitOptions get(LimitOffset limitOffset, boolean distinctValues) {
        if (distinctValues) {
            assert limitOffset.getLimit() > 0;
            return HASLIMITDISTINCTVALUES;
        }
        return limitOffset.getLimit() > 0 ? (limitOffset.getOffset() > 0 ? HASLIMITOFFSET : HASLIMIT) : NOLIMIT;
    }

    public static LimitOptions get(LimitOffset limitOffset) {
        return get(limitOffset, false);
    }

    public boolean hasLimit() {
        return this == HASLIMITOFFSET || this == HASLIMIT || this == HASLIMITDISTINCTVALUES;
    }

    public boolean hasOffset() {
        return this == HASLIMITOFFSET;
    }

    public boolean isDistinctValues() {
        return this == HASLIMITDISTINCTVALUES;
    }

    public String getLimit() {
        return hasLimit() ? SQLSession.limitParam : "";
    }

    public String getOffset() {
        return hasOffset() ? SQLSession.offsetParam : "";
    }
}
