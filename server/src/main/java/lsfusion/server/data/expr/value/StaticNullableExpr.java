package lsfusion.server.data.expr.value;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.join.base.ValueJoin;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public abstract class StaticNullableExpr extends NullableExpr implements StaticExprInterface {

    protected final AndClassSet paramClass;

    public StaticNullableExpr(AndClassSet paramClass) {
        this.paramClass = paramClass;
    }

    protected StaticNullableExpr translate(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return paramClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return paramClass.getTypeStat(forJoin);
    }

    public Expr translate(ExprTranslator translator) {
        return this;
    }
    public class NotNull extends NullableExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            return new ClassExprWhere(StaticNullableExpr.this, paramClass);
        }
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return PropStat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance(this);
    }
}
