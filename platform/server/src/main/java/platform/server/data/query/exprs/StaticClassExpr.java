package platform.server.data.query.exprs;

import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.where.Where;

public abstract class StaticClassExpr extends AndExpr {

    public abstract ConcreteClass getStaticClass();

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return getStaticClass().inSet(classes)?ClassExprWhere.TRUE:ClassExprWhere.FALSE;
    }

    public SourceExpr getClassExpr(BaseClass baseClass) {
        return ((ConcreteObjectClass)getStaticClass()).getIDExpr();
    }

    public Where getIsClassWhere(AndClassSet set) {
        return getStaticClass().inSet(set)?Where.TRUE:Where.FALSE;
    }
}
