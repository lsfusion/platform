package lsfusion.server.data.expr;

import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.ParseValue;

public abstract class AbstractValueExpr<C extends ConcreteClass> extends StaticExpr<C> implements ParseValue {

    public AbstractValueExpr(C objectClass) {
        super(objectClass);
    }

    @Override
    public boolean isAlwaysSafeString() {
        return objectClass.getType().isSafeString(null);
    }
}
