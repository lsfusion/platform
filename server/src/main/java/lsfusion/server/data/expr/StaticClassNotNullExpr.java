package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.ObjectClassField;

public abstract class StaticClassNotNullExpr extends NotNullExpr  implements StaticClassExprInterface {

    public abstract ConcreteClass getStaticClass();

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
    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
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

    @IdentityLazy
    private Where getCommonWhere() {
        return getNotNullWhere(getBaseJoin().getJoins().values());
    }

    private class NotNull extends NotNullExpr.NotNull {

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            return super.groupJoinsWheres(keepStat, keyStat, orderTop, type).and(getCommonWhere().groupJoinsWheres(keepStat, keyStat, orderTop, type));
        }

        public ClassExprWhere calculateClassWhere() {
            return getCommonWhere().getClassWhere();
        }
    }

    protected abstract ImCol<Expr> getParams();

    protected boolean hasUnionNotNull() { // можно было бы просто hasNotNull использовать, но там рекурсивный вызов большой, ну и можно было бы calculateNotNullWhere перегрузить, но хочется getCommonWhere в одном месте оставить
        return true;
    }

    public Where calculateNotNullWhere() { // overrided in FormulaUnionExpr, из-за отсутствия множественного наследования
        if(hasUnionNotNull())
            return new NotNull();
        else
            return getCommonWhere();
    }

}
