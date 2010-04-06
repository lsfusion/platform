package platform.server.data.type;

import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;

import java.util.Map;

public class NullReader implements Reader<Object> {

    public static NullReader instance = new NullReader();

    public Object read(Object value) {
        assert value==null;
        return null;
    }

    public void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, Where where) {
        return baseClass.unknown;
    }
}
