package lsfusion.server.data.type.reader;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.user.BaseClass;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NullReader extends AbstractReader<Object> implements ClassReader<Object> {

    public static NullReader instance = new NullReader();

    public static Type typeInstance = IntegerClass.instance;

    public static <K> Type.Getter<K> typeGetter(final ImMap<K, ? extends Reader> map) {
        return key -> {
            Reader reader = map.get(key);
            if (reader instanceof Type)
                return (Type) reader;
            return NullReader.typeInstance;
        };
    }

    @Override
    public Object read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
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
