package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.classes.ConcreteClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public abstract class AbstractValueExpr extends StaticClassExpr {

    public final Object object;
    public final ConcreteClass objectClass;

    public AbstractValueExpr(Object object, ConcreteClass objectClass) {
        this.object = object;
        this.objectClass = objectClass;

        assert !(this.objectClass instanceof LogicalClass && !this.object.equals(true));
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public Type getType(KeyType keyType) {
        return objectClass.getType();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean twins(TwinImmutableInterface o) {
        return object.equals(((AbstractValueExpr)o).object) && objectClass.equals(((AbstractValueExpr)o).objectClass);
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public long calculateComplexity() {
        return 1;
    }
}
