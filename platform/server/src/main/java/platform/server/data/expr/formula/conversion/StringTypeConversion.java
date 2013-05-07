package platform.server.data.expr.formula.conversion;

import platform.server.classes.InsensitiveStringClass;
import platform.server.classes.StringClass;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class StringTypeConversion implements TypeConversion {
    public final static StringTypeConversion instance = new StringTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 instanceof InsensitiveStringClass || type2 instanceof InsensitiveStringClass) {
            int length1 = type1 == null ? 0 : type1.getBinaryLength(true);
            int length2 = type2 == null ? 0 : type2.getBinaryLength(true);
            return InsensitiveStringClass.get(length1 + length2);
        }
        if (type1 instanceof StringClass || type2 instanceof StringClass) {
            int length1 = type1 == null ? 0 : type1.getBinaryLength(true);
            int length2 = type2 == null ? 0 : type2.getBinaryLength(true);
            return StringClass.get(length1 + length2);
        }
        return null;
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        Type type = getType(type1, type2);
        if (type != null) {
            if (!(type1 instanceof StringClass)) {
                src1 = type.getCast(src1, compile.syntax, false);
            }
            if (!(type2 instanceof StringClass)) {
                src2 = type.getCast(src2, compile.syntax, false);
            }

            return "(rtrim(" + src1 + ") || rtrim(" + src2 + "))";
        }
        return null;
    }
}
