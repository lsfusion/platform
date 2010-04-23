package platform.server.data.type;

import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.SQLSession;
import platform.server.data.sql.SQLSyntax;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.logics.ObjectValue;

import java.util.Map;

public interface Reader<T> {
    T read(Object value);

    void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass);
    ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, Where where);
}
