package platform.server.data.expr;

import platform.server.classes.ConcreteClass;
import platform.server.data.where.MapWhere;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public abstract class StaticExpr<C extends ConcreteClass> extends StaticClassExpr {

    public final C objectClass;

    public StaticExpr(C objectClass) {
        this.objectClass = objectClass;
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
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

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public long calculateComplexity() {
        return 1;
    }

    public void enumDepends(ExprEnumerator enumerator) {
    }
}
