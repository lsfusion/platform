package lsfusion.server.data.type.reader;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.ConcreteClass;

public interface ClassReader<T> extends Reader<T> {
    void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass);
    ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType);
}
