package lsfusion.server.data.query.compile;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.query.Depth;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.base.caches.ManualLazy;

public class SubQueryContext extends TwinImmutableObject {

    private Depth subQueryDepth;
    @ManualLazy
    public Depth getSubQueryDepth() {
        if(subQueryDepth == null) {
            Settings settings = Settings.get();
            if(subQuery < settings.getSubQueryLargeDepth())
                subQueryDepth = Depth.NORMAL;
            else if(subQuery < settings.getSubQueryInfiniteDepth())
                subQueryDepth = Depth.LARGE;
            else
                subQueryDepth = Depth.INFINITE;
        }
        return subQueryDepth;
    }

    public final static SubQueryContext EMPTY = new SubQueryContext(0, 0, 0, 0, 0);
    
    private final int alias, recursion, siblingSubQuery, subQueryExprs;
    
    // нельзя добавлять в equals напрямую так как с кэшами будут проблемы, но так проблемы могут быть с недетерминированностью  
    private final int subQuery; // volatile, same as SessionTable.count

    private SubQueryContext(int alias, int recursion, int siblingSubQuery, int subQuery, int subQueryExprs) {
        this.alias = alias;
        this.recursion = recursion;
        this.siblingSubQuery = siblingSubQuery;
        this.subQuery = subQuery;
        this.subQueryExprs = subQueryExprs;
        assert alias >= 0;
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
    
    public int getSubQueryExprs() {
        return subQueryExprs;
    }

    public final static int MAX_PUSH = 23;
    public SubQueryContext pushAlias(int shift) {
        assert shift < MAX_PUSH;
        return new SubQueryContext(alias * MAX_PUSH + shift, recursion, siblingSubQuery, subQuery, subQueryExprs);
    }

    public SubQueryContext pushRecursion() {
        return new SubQueryContext(alias, recursion + 1, siblingSubQuery, subQuery, subQueryExprs);
    }

    public SubQueryContext pushSubQueryExprs() {
        return new SubQueryContext(alias, recursion, siblingSubQuery, subQuery, subQueryExprs + 1);
    }

    public SubQueryContext pushSiblingSubQuery() {
        return new SubQueryContext(alias, recursion, siblingSubQuery + 1, subQuery, subQueryExprs);
    }

    public SubQueryContext pushSubQuery() {
        return new SubQueryContext(alias, recursion, siblingSubQuery, subQuery + 1, subQueryExprs);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return alias == ((SubQueryContext)o).alias && recursion == ((SubQueryContext)o).recursion  && siblingSubQuery == ((SubQueryContext)o).siblingSubQuery && subQueryExprs == ((SubQueryContext)o).subQueryExprs && getSubQueryDepth() == ((SubQueryContext)o).getSubQueryDepth();
    }

    public int immutableHashCode() {
        return 31 * (31 * ((alias * 31 + recursion) * 31 + siblingSubQuery) + getSubQueryDepth().hashCode()) + subQueryExprs;
    }
}
