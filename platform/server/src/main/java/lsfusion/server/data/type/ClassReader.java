package lsfusion.server.data.type;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.where.Where;

public interface ClassReader<T> extends Reader<T> {
    void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass);
    ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType);
}
