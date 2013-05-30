package platform.server.data.type;

import platform.base.ExtInt;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.where.Where;

public class NullReader extends AbstractReader<Object> implements ClassReader<Object> {

    public static NullReader instance = new NullReader();

    public Object read(Object value) {
        assert value==null;
        return null;
    }

    public void prepareClassesQuery(Expr expr, Where where, MSet<Expr> exprs, BaseClass baseClass) {
    }

    public ConcreteClass readClass(Expr expr, ImMap<Expr, Object> classes, BaseClass baseClass, KeyType keyType) {
        return baseClass.unknown;
    }

    public ExtInt getCharLength() {
        return new ExtInt(5);
    }
}
