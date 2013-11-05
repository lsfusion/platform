package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.ValueJoin;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class CurrentEnvironmentExpr extends NotNullExpr {

    private final String paramString;
    private final AndClassSet paramClass;

    public CurrentEnvironmentExpr(String paramString, AndClassSet paramClass) {
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    protected CurrentEnvironmentExpr translate(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return paramClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return paramClass.getTypeStat();
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public boolean twins(TwinImmutableObject obj) {
        return paramString.equals(((CurrentEnvironmentExpr) obj).paramString);
    }

    protected int hash(HashContext hashContext) {
        return paramString.hashCode();
    }

    public String getSource(CompileSource compile) {
        return paramString;
    }

    public class NotNull extends NotNullExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            return new ClassExprWhere(CurrentEnvironmentExpr.this, paramClass);
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            return new GroupJoinsWheres(this, type);
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
