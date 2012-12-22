package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.hash.HashContext;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.ValueJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

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

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
            return new GroupJoinsWheres(this, noWhere);
        }
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public Stat getStatValue(KeyStat keyStat) {
        return Stat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance;
    }
}
