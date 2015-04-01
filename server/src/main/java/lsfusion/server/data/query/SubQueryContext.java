package lsfusion.server.data.query;

import lsfusion.base.TwinImmutableObject;

public class SubQueryContext extends TwinImmutableObject {

    public final static SubQueryContext EMPTY = new SubQueryContext(0, 0, 0);
    
    private final int alias, recursion, siblingSubQuery;

    public SubQueryContext(int alias, int recursion, int siblingSubQuery) {
        this.alias = alias;
        this.recursion = recursion;
        this.siblingSubQuery = siblingSubQuery;
    }

    public String wrapAlias(String name) {
        if(alias==0) // чисто для красоты
            return name;
        return "w" + alias + name;
    }

    public String wrapRecursion(String name) {
        return "w" + recursion + name;
    }

    public String wrapSiblingSubQuery(String name) {
        return "w" + siblingSubQuery + name;
    }

    public final static int MAX_PUSH = 23;
    public SubQueryContext pushAlias(int shift) {
        assert shift < MAX_PUSH;
        return new SubQueryContext(alias * MAX_PUSH + shift, recursion, siblingSubQuery);
    }

    public SubQueryContext pushRecursion() {
        return new SubQueryContext(alias, recursion + 1, siblingSubQuery);
    }

    public SubQueryContext pushSiblingSubQuery() {
        return new SubQueryContext(alias, recursion, siblingSubQuery + 1);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return alias == ((SubQueryContext)o).alias && recursion == ((SubQueryContext)o).recursion  && siblingSubQuery == ((SubQueryContext)o).siblingSubQuery;
    }

    public int immutableHashCode() {
        return (alias * 31 + recursion) * 31 + siblingSubQuery;
    }
}
