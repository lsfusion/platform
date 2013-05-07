package platform.server.data.expr.formula.conversion;

import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class CompatibleTypeConversion implements TypeConversion {
    public final static CompatibleTypeConversion instance = new CompatibleTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type2 == null) return type1;
        if (type1 == null) return type2;

        return type1.getCompatible(type2);
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        return null;
    }
}
