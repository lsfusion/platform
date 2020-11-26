package lsfusion.server.data.expr.value;

import lsfusion.server.data.query.compile.ParseValue;
import lsfusion.server.logics.classes.ConcreteClass;

public abstract class AbstractValueExpr<C extends ConcreteClass> extends StaticExpr<C> implements ParseValue {

    public AbstractValueExpr(C objectClass) {
        super(objectClass);
    }

    @Override
    public boolean isAlwaysSafeString() {
        return objectClass.getType().isSafeString(null);
    }

    public abstract Object getObject();
}
