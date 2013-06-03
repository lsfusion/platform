package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.where.Where;

public class GroupJoinsWhere extends GroupWhere<GroupJoinsWhere> {

    public final WhereJoins joins;

    public final ImMap<WhereJoin, Where> upWheres;

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, ImMap<WhereJoin, Where> upWheres, Where where) {
        this(keyEqual, joins, where, upWheres);

        assert where.getKeyEquals().singleKey().isEmpty();
    }

    // конструктор паковки для assertion'а
    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Where where, ImMap<WhereJoin, Where> upWheres) {
        super(keyEqual, where);
        this.joins = joins;
        this.upWheres = upWheres;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups) {
        return joins.getStatKeys(groups, where, keyEqual);
    }

    public boolean isComplex() {
        return getComplexity(false) > Settings.get().getLimitWhereJoinPack();
    }

    @Override
    public boolean twins(TwinImmutableObject o) {
        return super.twins(o) && joins.equals(((GroupJoinsWhere) o).joins) && upWheres.equals(((GroupJoinsWhere) o).upWheres);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * super.immutableHashCode() + joins.hashCode()) + upWheres.hashCode();
    }
}
