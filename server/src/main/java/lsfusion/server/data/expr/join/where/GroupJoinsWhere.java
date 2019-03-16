package lsfusion.server.data.expr.join.where;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.logging.DebugInfoWriter;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.where.Where;

public class GroupJoinsWhere extends GroupWhere<GroupJoinsWhere> {

    public final WhereJoins joins;

    public final UpWheres<WhereJoin> upWheres; // used only for compilation

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, UpWheres<WhereJoin> upWheres, Where where, ImOrderSet<Expr> orderTop) {
        this(keyEqual, joins, where, upWheres);

        assert !orderTop.isEmpty() || where.getKeyEquals().singleKey().isEmpty(); // из-за symmetricWhere в groupNotJoinsWheres
    }

    // конструктор паковки для assertion'а
    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Where where, UpWheres<WhereJoin> upWheres) {
        super(keyEqual, where);
        this.joins = joins;
        this.upWheres = upWheres;
    }

    public <K extends BaseExpr, PK extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, StatType type, StatKeys<PK> pushStatKeys) {
        return joins.pushStatKeys(pushStatKeys).getStatKeys(groups, where, type, keyEqual);
    }
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, StatType type) {
        return getStatKeys(groups, type, StatKeys.<KeyExpr>NOPUSH());
    }

    public <K extends BaseExpr, Z extends Expr> Where getCostPushWhere(final QueryJoin<Z, ?, ?, ?> queryJoin, boolean pushLargeDepth, final StatType type, DebugInfoWriter debugInfoWriter) {
        return joins.getCostPushWhere(queryJoin, pushLargeDepth, upWheres, where, type, keyEqual, debugInfoWriter);   
    }

    @Override
    public String toString() {
        return joins.toString() + " " + keyEqual;
    }

    public boolean isComplex() {
        return getComplexity(false) > Settings.get().getLimitWhereJoinPack();
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && joins.equals(((GroupJoinsWhere) o).joins) && upWheres.equals(((GroupJoinsWhere) o).upWheres);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * super.immutableHashCode() + joins.hashCode()) + upWheres.hashCode();
    }

}
