package platform.server.data.expr;

import platform.server.classes.ConcreteClass;

public abstract class VariableClassExpr extends BaseExpr {

    public ConcreteClass getStaticClass() {
        return null;
    }
}
