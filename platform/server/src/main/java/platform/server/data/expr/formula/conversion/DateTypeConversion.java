package platform.server.data.expr.formula.conversion;

import platform.server.classes.DateClass;
import platform.server.classes.IntegerClass;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class DateTypeConversion implements TypeConversion {
    public final static DateTypeConversion instance = new DateTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 == DateClass.instance && type2 == IntegerClass.instance) {
            return DateClass.instance;
        }
        return null;
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        if (getType(type1, type2) != null) {
            return "(" + src1 + " + " + src2 + ")";
        }
        return null;
    }
}
