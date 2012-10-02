package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.CalculateJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.type.Type;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

// выражение для оптимизации, разворачивание которого в case'ы даст экспоненту
public abstract class UnionExpr extends NotNullExpr implements StaticClassExprInterface {

    public abstract DataClass getStaticClass();

    protected abstract Set<Expr> getParams();

    public Type getType(KeyType keyType) {
        return getStaticClass();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    public Where calculateOrWhere() {
        Where result = Where.FALSE;
        for(Expr operand : getParams())
            result = result.or(operand.getWhere());
        return result;
    }

    @Override
    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(getParams());
    }

    @Override
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(Expr operand : getParams()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    protected boolean isComplex() {
        return true;
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Stat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    @IdentityLazy
    public UnionJoin getBaseJoin() {
        return new UnionJoin(getParams()); // ??? тут надо было бы getTypeStat использовать, но пока не предполагается использование Linear в Join'ах
    }

    // множественное наследование StaticClassExpr
    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return StaticClassExpr.getClassWhere(this, classes);
    }

    @Override
    public Expr classExpr(BaseClass baseClass) {
        return StaticClassExpr.classExpr(this, baseClass);
    }

    @Override
    public Where isClass(AndClassSet set) {
        return StaticClassExpr.isClass(this, set);
    }

    @Override
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return StaticClassExpr.getAndClassSet(this, and);
    }

    @Override
    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        return StaticClassExpr.addAndClassSet(this, add);
    }
}
