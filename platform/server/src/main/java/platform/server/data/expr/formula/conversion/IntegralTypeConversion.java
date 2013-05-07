package platform.server.data.expr.formula.conversion;

import platform.server.classes.IntegralClass;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class IntegralTypeConversion implements TypeConversion {
    public final static IntegralTypeConversion instance = new IntegralTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 == null && type2 instanceof IntegralClass) {
            return type2;
        }

        if (type2 == null && type1 instanceof IntegralClass) {
            return type1;
        }

        if (type1 instanceof IntegralClass && type2 instanceof IntegralClass) {
            return type1.getCompatible(type2);
        }

        return null;
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        Type type = getType(type1, type2);
        if (type != null) {
            return "(" + src1 + "+" + src2 + ")";
        }
        return null;
    }
}
