package lsfusion.server.data.query;

import lsfusion.server.data.SQLSession;

public class LimitOptions {

    public final static LimitOptions HASLIMIT = new LimitOptions();
    public final static LimitOptions NOLIMIT = new LimitOptions();

    public static LimitOptions get(int top) {
        return top > 0 ? HASLIMIT : NOLIMIT;
    }

    public boolean hasLimit() {
        return this == HASLIMIT;
    }

    public String getString() {
        return hasLimit() ? SQLSession.limitParam : "";
    }
}
