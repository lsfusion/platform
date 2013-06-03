package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.type.Type;

public class CompoundTypeConversion implements TypeConversion {

    protected final TypeConversion conversions[];

    public CompoundTypeConversion(TypeConversion... conversions) {
        this.conversions = conversions;
    }

    @Override
    public Type getType(Type type1, Type type2) {
        Type result = null;
        for (TypeConversion conversion : conversions) {
            Type conversionType = conversion.getType(type1, type2);
            if (conversionType != null) {
                result = conversionType;
                break;
            }
        }
        return result;
    }
}
