package platform.server.data.expr;

import platform.server.classes.ConcreteClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.DoubleClass;
import platform.server.data.where.Where;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.MapTranslate;
import platform.server.caches.hash.HashContext;
import platform.base.BaseUtils;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

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

    public boolean twins(AbstractSourceJoin o) {
        return object.equals(((AbstractValueExpr)o).object) && objectClass.equals(((AbstractValueExpr)o).objectClass);
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public long calculateComplexity() {
        return 1;
    }
}
