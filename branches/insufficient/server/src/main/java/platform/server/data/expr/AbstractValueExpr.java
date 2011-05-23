package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.classes.ConcreteClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public abstract class AbstractValueExpr extends StaticExpr<ConcreteClass> {

    public final Object object;

    public AbstractValueExpr(Object object, ConcreteClass objectClass) {
        super(objectClass);
        this.object = object;

        assert !(this.objectClass instanceof LogicalClass && !this.object.equals(true));
    }

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public boolean twins(TwinImmutableInterface o) {
        return object.equals(((AbstractValueExpr)o).object) && objectClass.equals(((AbstractValueExpr)o).objectClass);
    }
}
