package platform.server.data.type;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.QueryBuilder;
import platform.server.data.where.Where;

public interface ClassReader<T> extends Reader<T> {
    void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass);
    ConcreteClass readClass(Expr expr, ImMap<Object, Object> classes, BaseClass baseClass, KeyType keyType);
}
