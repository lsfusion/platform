package lsfusion.server.data.type;

import lsfusion.base.ExtInt;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.where.Where;

public class NullReader extends AbstractReader<Object> implements ClassReader<Object> {

    public static NullReader instance = new NullReader();

    public static Type typeInstance = IntegerClass.instance;

    public static <K> Type.Getter<K> typeGetter(final ImMap<K, ? extends Reader> map) {
        return new Type.Getter<K>() {
            public Type getType(K key) {
                Reader reader = map.get(key);
                if (reader instanceof Type)
                    return (Type) reader;
                return NullReader.typeInstance;
            }
        };
    }

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
