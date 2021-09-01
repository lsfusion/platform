package lsfusion.server.data.query;

import lsfusion.server.data.sql.SQLSession;

public class LimitOptions {

    public final static LimitOptions HASLIMITDISTINCTVALUES = new LimitOptions();
    public final static LimitOptions HASLIMIT = new LimitOptions();
    public final static LimitOptions NOLIMIT = new LimitOptions();

    public static LimitOptions get(int top, boolean distinctValues) {
        if(distinctValues) {
            assert top > 0;
            return HASLIMITDISTINCTVALUES;
        }
        return top > 0 ? HASLIMIT : NOLIMIT;
    }
    public static LimitOptions get(int top) {
        return get(top, false);
    }

    public boolean hasLimit() {
        return this == HASLIMIT || this == HASLIMITDISTINCTVALUES;
    }
    
    public boolean isDistinctValues() {
        return this == HASLIMITDISTINCTVALUES;
    }

    public String getString() {
        return hasLimit() ? SQLSession.limitParam : "";
    }
}
