package platform.server.data.query.innerjoins;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.Settings;
import platform.server.caches.AbstractTranslateContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackInterface;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.where.Where;

import java.util.*;

public class GroupJoinsWhere extends GroupWhere<GroupJoinsWhere> {

    public final WhereJoins joins;

    public final Map<WhereJoin, Where> upWheres;

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Map<WhereJoin, Where> upWheres, Where where) {
        this(keyEqual, joins, where, upWheres);

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }

    // конструктор паковки для assertion'а
    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Where where, Map<WhereJoin, Where> upWheres) {
        super(keyEqual, where);
        this.joins = joins;
        this.upWheres = upWheres;
    }

    public static long getComplexity(Collection<GroupJoinsWhere> whereJoins, boolean outer) {
        int complexity = 0;
        for(GroupJoinsWhere whereJoin : whereJoins)
            complexity += whereJoin.where.getComplexity(outer);
        return complexity;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> groups) {
        return joins.getStatKeys(groups, where, keyEqual);
    }

    public boolean isComplex() {
        return getComplexity(false) > Settings.instance.getLimitWhereJoinPack();
    }

    @Override
    public boolean twins(TwinImmutableInterface o) {
        return super.twins(o) && joins.equals(((GroupJoinsWhere) o).joins) && upWheres.equals(((GroupJoinsWhere) o).upWheres);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * super.immutableHashCode() + joins.hashCode()) + upWheres.hashCode();
    }
}
