package platform.server.data.expr;

import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.Where;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;

public abstract class StaticClassExpr extends BaseExpr {

    public abstract ConcreteClass getStaticClass();

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return getStaticClass().inSet(classes)?ClassExprWhere.TRUE:ClassExprWhere.FALSE;
    }

    public Expr classExpr(BaseClass baseClass) {
        return ((ConcreteObjectClass)getStaticClass()).getIDExpr();
    }

    public Where isClass(AndClassSet set) {
        return getStaticClass().inSet(set)?Where.TRUE:Where.FALSE;
    }
}
