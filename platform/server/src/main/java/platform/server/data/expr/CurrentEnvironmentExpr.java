package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.ValueJoin;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Set;

public class CurrentEnvironmentExpr extends NotNullExpr {

    private final String paramString;
    private final AndClassSet paramClass;

    public CurrentEnvironmentExpr(String paramString, AndClassSet paramClass) {
        this.paramString = paramString;
        this.paramClass = paramClass;
    }

    public void fillFollowSet(DataWhereSet fillSet) {
    }

    public CurrentEnvironmentExpr translateOuter(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
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

    public boolean twins(TwinImmutableInterface obj) {
        return paramString.equals(((CurrentEnvironmentExpr) obj).paramString);
    }

    public int hashOuter(HashContext hashContext) {
        return paramString.hashCode();
    }

    public String getSource(CompileSource compile) {
        return paramString;
    }

    public void enumDepends(ExprEnumerator enumerator) {
    }

    public long calculateComplexity() {
        return 1;
    }

    public class NotNull extends NotNullExpr.NotNull {
        protected DataWhereSet calculateFollows() {
            return new DataWhereSet();
        }

        public ClassExprWhere calculateClassWhere() {
            return new ClassExprWhere(CurrentEnvironmentExpr.this, paramClass);
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(Set<K> keepStat, KeyStat keyStat) {
            return new GroupJoinsWheres(this);
        }
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    public Stat getStatValue(KeyStat keyStat) {
        return Stat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance;
    }
}
