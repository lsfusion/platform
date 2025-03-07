package lsfusion.server.logics.form.stat;

public class LimitOffset {

    public final static LimitOffset NOLIMIT = new LimitOffset(0, 0);

    private final int limit;
    private final int offset;

    public LimitOffset(int limit) {
        this(limit, 0);
    }

    public  LimitOffset(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
