package lsfusion.server.data.query;

import lsfusion.base.TwinImmutableObject;

public class SubQueryContext extends TwinImmutableObject {

    public final static SubQueryContext EMPTY = new SubQueryContext(0, 0);
    
    private final int alias, recursion;

    public SubQueryContext(int alias, int recursion) {
        this.alias = alias;
        this.recursion = recursion;
    }

    public String wrapAlias(String name) {
        if(alias==0) // чисто для красоты
            return name;
        return "w" + alias + name;
    }

    public String wrapRecursion(String name) {
        return "w" + recursion + name;
    }

    public final static int MAX_PUSH = 23;
    public SubQueryContext pushAlias(int shift) {
        assert shift < MAX_PUSH;
        return new SubQueryContext(alias * MAX_PUSH + shift, recursion);
    }

    public SubQueryContext pushRecursion() {
        return new SubQueryContext(alias, recursion + 1);
    }

    public boolean twins(TwinImmutableObject o) {
        return alias == ((SubQueryContext)o).alias && recursion == ((SubQueryContext)o).recursion;
    }

    public int immutableHashCode() {
        return alias * 31 + recursion;
    }
}
