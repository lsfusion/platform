package platform.server.data.where.classes;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.List;

// упрощенный Where
public class PackClassWhere extends DataWhere {

    ClassExprWhere packWhere;

    public PackClassWhere(ClassExprWhere packWhere) {
        this.packWhere = packWhere;

        assert !packWhere.isFalse();
        assert !packWhere.isTrue();
    }

    protected DataWhereSet calculateFollows() {
        return new DataWhereSet(packWhere.getExprFollows());
    }

    public QuickSet<OuterContext> calculateOuterDepends() {
        return QuickSet.EMPTY();
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        throw new RuntimeException("Not supported");
    }

    public int hash(HashContext hashContext) {
        return System.identityHashCode(this);
    }

    public boolean twins(TwinImmutableInterface obj) {
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

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<Expr> orderTop, boolean noWhere) {
        throw new RuntimeException("Not supported");
    }
    public ClassExprWhere calculateClassWhere() {
        return packWhere;
    }

}
