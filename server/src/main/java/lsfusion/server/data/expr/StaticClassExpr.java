package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.FormulaExprInterface;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.ObjectClassField;

public abstract class StaticClassExpr extends BaseExpr implements StaticClassExprInterface {

    public static ClassExprWhere getClassWhere(StaticClassExprInterface expr, AndClassSet classes) {
        return expr.getStaticClass().inSet(classes)?ClassExprWhere.TRUE:ClassExprWhere.FALSE;
    }
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return getClassWhere(this, classes);
    }

    public static Expr getClassExpr(ConcreteObjectClass staticClass) {
        return staticClass.getClassObject().getStaticExpr();        
    }
    public static Expr classExpr(StaticClassExprInterface expr, ImSet<ObjectClassField> classTables, IsClassType type) {
        ConcreteObjectClass staticClass = (ConcreteObjectClass) expr.getStaticClass();
        if(!IsClassExpr.inSet(staticClass, classTables))
            return Expr.NULL;
        return getClassExpr(staticClass);
    }
    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
        return classExpr(this, classes, type);
    }

    private final static DataClass defaultUnknownFormulaStaticClass = IntegerClass.instance;
    
    public static Where isClass(StaticClassExprInterface expr, AndClassSet set, boolean notConsistent) {
        ConcreteClass staticClass = expr.getStaticClass();
        // тут конечно из-за отсутствия keyType могут быть чудеса вроде f(a) <- b (+) 5 WHERE g(a) или просто (b (+) NULL) AS  NUMERIC
        if(staticClass == null && (expr instanceof LinearExpr || expr instanceof FormulaExprInterface)) // поэтому пока вставим подстраховку от таких случаев, чтобы не падала
            staticClass = defaultUnknownFormulaStaticClass;
        return staticClass.inSet(set)?Where.TRUE:Where.FALSE;
    }
    public Where isClass(ValueClassSet set, boolean inconsistent) {
        return isClass(this, set, inconsistent);
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
