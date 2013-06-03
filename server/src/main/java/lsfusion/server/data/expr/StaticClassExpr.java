package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.ClassField;

public abstract class StaticClassExpr extends BaseExpr implements StaticClassExprInterface {

    public static ClassExprWhere getClassWhere(StaticClassExprInterface expr, AndClassSet classes) {
        return expr.getStaticClass().inSet(classes)?ClassExprWhere.TRUE:ClassExprWhere.FALSE;
    }
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return getClassWhere(this, classes);
    }

    public static Expr classExpr(StaticClassExprInterface expr, ImSet<ClassField> classTables) {
        ConcreteObjectClass staticClass = (ConcreteObjectClass) expr.getStaticClass();
        if(!IsClassExpr.inSet(staticClass, classTables))
            return Expr.NULL;
        return staticClass.getClassObject().getStaticExpr();
    }
    public Expr classExpr(ImSet<ClassField> classes) {
        return classExpr(this, classes);
    }

    public static Where isClass(StaticClassExprInterface expr, AndClassSet set) {
        return expr.getStaticClass().inSet(set)?Where.TRUE:Where.FALSE;
    }
    public Where isClass(ValueClassSet set) {
        return isClass(this, set);
    }

    public static AndClassSet getAndClassSet(StaticClassExprInterface expr, ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return expr.getStaticClass();
    }
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return getAndClassSet(this, and);
    }

    public static boolean addAndClassSet(StaticClassExprInterface expr, AndClassSet add) {
        return expr.getStaticClass().inSet(add);
    }
    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        return addAndClassSet(this, add);
    }
}
