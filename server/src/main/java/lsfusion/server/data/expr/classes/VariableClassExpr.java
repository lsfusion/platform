package lsfusion.server.data.expr.classes;

import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.logics.classes.ConcreteClass;

public abstract class VariableClassExpr extends BaseExpr {

    public ConcreteClass getStaticClass() {
        return null;
    }
}
