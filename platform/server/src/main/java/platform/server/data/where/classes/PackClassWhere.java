package platform.server.data.where.classes;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;

// упрощенный Where
public class PackClassWhere extends DataWhere {

    ClassExprWhere packWhere;

    public PackClassWhere(ClassExprWhere packWhere) {
        this.packWhere = packWhere;

        assert !packWhere.isFalse();
        assert !packWhere.isTrue();
    }

    protected ImSet<DataWhere> calculateFollows() {
        return NotNullExpr.getFollows(packWhere.getExprFollows());
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.EMPTY();
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        throw new RuntimeException("Not supported");
    }

    public int hash(HashContext hashContext) {
        return System.identityHashCode(this);
    }

    public boolean twins(TwinImmutableObject obj) {
        return false;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return packWhere.toString();

        throw new RuntimeException("Not supported");
    }

    @Override
    public String toString() {
        return packWhere.toString();
    }

    protected Where translate(MapTranslate translator) {
        throw new RuntimeException("Not supported");
    }
    public Where translateQuery(QueryTranslator translator) {
        throw new RuntimeException("Not supported");
    }

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
        throw new RuntimeException("Not supported");
    }
    public ClassExprWhere calculateClassWhere() {
        return packWhere;
    }

}
