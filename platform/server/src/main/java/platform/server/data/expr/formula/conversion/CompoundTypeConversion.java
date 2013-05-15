package platform.server.data.expr.formula.conversion;

import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

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

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        String conversionSource = null;
        for (TypeConversion conversion : conversions) {
            conversionSource = conversion.getSource(compile, type1, type2, src1, src2);
            if (conversionSource != null) {
                break;
            }
        }
        return conversionSource;
    }
}
