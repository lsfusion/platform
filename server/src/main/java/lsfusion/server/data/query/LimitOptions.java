package lsfusion.server.data.query;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.form.stat.LimitOffset;

public class LimitOptions {

    public final static LimitOptions HASLIMITDISTINCTVALUES = new LimitOptions();
    public final static LimitOptions HASLIMIT = new LimitOptions();
    public final static LimitOptions HASLIMITOFFSET = new LimitOptions();
    public final static LimitOptions HASOFFSET = new LimitOptions();
    public final static LimitOptions NOLIMIT = new LimitOptions();

    public static LimitOptions get(LimitOffset limitOffset, boolean distinctValues) {
        boolean hasLimit = limitOffset.getLimit() > 0;
        boolean hasOffset = limitOffset.getOffset() > 0;
        if (distinctValues) {
            assert hasLimit && !hasOffset;
            return HASLIMITDISTINCTVALUES;
        }
        return hasLimit ? (hasOffset ? HASLIMITOFFSET : HASLIMIT) : (hasOffset ? HASOFFSET : NOLIMIT);
    }

    public static LimitOptions get(LimitOffset limitOffset) {
        return get(limitOffset, false);
    }

    public boolean hasLimit() {
        return this == HASLIMITOFFSET || this == HASLIMIT || this == HASLIMITDISTINCTVALUES;
    }

    public boolean hasOffset() {
        return this == HASLIMITOFFSET || this == HASOFFSET;
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
