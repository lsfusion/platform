package platform.server.data.type;

import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.Query;

import java.util.Map;

public class NullReader implements ClassReader<Object> {

    public static NullReader instance = new NullReader();

    public Object read(Object value) {
        assert value==null;
        return null;
    }

    public void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, KeyType keyType) {
        return baseClass.unknown;
    }
}
