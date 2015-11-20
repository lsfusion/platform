package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.type.Type;

public abstract class IntegralTypeConversion implements TypeConversion {

    public final static IntegralTypeConversion sumTypeConversion = new IntegralTypeConversion() {
        public IntegralClass getIntegralClass(IntegralClass type1, IntegralClass type2) {
            return (IntegralClass) type1.getCompatible(type2);
        }
    };

    public abstract IntegralClass getIntegralClass(IntegralClass type1, IntegralClass type2);
    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 == null && type2 instanceof IntegralClass) {
            return type2;
        }

        if (type2 == null && type1 instanceof IntegralClass) {
            return type1;
        }

        if (type1 instanceof IntegralClass && type2 instanceof IntegralClass) {
            return getIntegralClass((IntegralClass) type1, (IntegralClass) type2);
        }

        return null;
    }
}
