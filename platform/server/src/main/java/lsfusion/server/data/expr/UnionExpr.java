package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.ClassField;

// выражение для оптимизации, разворачивание которого в case'ы даст экспоненту
public abstract class UnionExpr extends NotNullExpr implements StaticClassExprInterface {

    public abstract DataClass getStaticClass();

    protected abstract ImSet<Expr> getParams();

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
    public ImSet<OuterContext> calculateOuterDepends() {
        return BaseUtils.immutableCast(getParams());
    }

    @Override
    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        for(Expr operand : getParams()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    protected boolean isComplex() {
        return true;
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public PropStat getStatValue(KeyStat keyStat) {
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
    public Expr classExpr(ImSet<ClassField> classes) {
        return StaticClassExpr.classExpr(this, classes);
    }

    @Override
    public Where isClass(ValueClassSet set) {
        return StaticClassExpr.isClass(this, set);
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return StaticClassExpr.getAndClassSet(this, and);
    }

    @Override
    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        return StaticClassExpr.addAndClassSet(this, add);
    }
}
