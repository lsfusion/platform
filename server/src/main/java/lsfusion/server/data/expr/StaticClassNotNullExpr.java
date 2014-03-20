package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.classes.ConcreteClass;
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

public abstract class StaticClassNotNullExpr extends NotNullExpr  implements StaticClassExprInterface {

    public abstract ConcreteClass getStaticClass();

    protected abstract ImCol<Expr> getParams();

    public Type getType(KeyType keyType) {
        return getStaticClass().getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return getStaticClass().getTypeStat(forJoin);
    }

    @Override
    public ImSet<OuterContext> calculateOuterDepends() {
        return BaseUtils.immutableCast(getParams().toSet());
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    // множественное наследование StaticClassExpr
    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return StaticClassExpr.getClassWhere(this, classes);
    }

    @Override
    public Expr classExpr(ImSet<ClassField> classes, IsClassType type) {
        return StaticClassExpr.classExpr(this, classes, type);
    }

    @Override
    public Where isClass(ValueClassSet set, boolean inconsistent) {
        return StaticClassExpr.isClass(this, set, inconsistent);
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
