package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.ValueJoin;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public abstract class StaticNotNullExpr extends NotNullExpr {

    private final AndClassSet paramClass;

    public StaticNotNullExpr(AndClassSet paramClass) {
        this.paramClass = paramClass;
    }

    protected StaticNotNullExpr translate(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return paramClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return paramClass.getTypeStat(forJoin);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }
    public class NotNull extends NotNullExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            return new ClassExprWhere(StaticNotNullExpr.this, paramClass);
        }
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return PropStat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance;
    }

}
