package platform.server.data.expr;

import platform.base.QuickMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

public abstract class StaticClassExpr extends BaseExpr implements StaticClassExprInterface {

    public static ClassExprWhere getClassWhere(StaticClassExprInterface expr, AndClassSet classes) {
        return expr.getStaticClass().inSet(classes)?ClassExprWhere.TRUE:ClassExprWhere.FALSE;
    }
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return getClassWhere(this, classes);
    }

    public static Expr classExpr(StaticClassExprInterface expr, BaseClass baseClass) {
        return ((ConcreteObjectClass)expr.getStaticClass()).getClassObject().getStaticExpr();
    }
    public Expr classExpr(BaseClass baseClass) {
        return classExpr(this, baseClass);
    }

    public static Where isClass(StaticClassExprInterface expr, AndClassSet set) {
        return expr.getStaticClass().inSet(set)?Where.TRUE:Where.FALSE;
    }
    public Where isClass(AndClassSet set) {
        return isClass(this, set);
    }

    public static AndClassSet getAndClassSet(StaticClassExprInterface expr, QuickMap<VariableClassExpr, AndClassSet> and) {
        return expr.getStaticClass();
    }
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return getAndClassSet(this, and);
    }

    public static boolean addAndClassSet(StaticClassExprInterface expr, AndClassSet add) {
        return expr.getStaticClass().inSet(add);
    }
    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        return addAndClassSet(this, add);
    }
}
