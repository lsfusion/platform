package platform.server.data.expr.formula.conversion;

import platform.server.classes.TextClass;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class TextTypeConversion implements TypeConversion {
    public final static TextTypeConversion instance = new TextTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 == TextClass.instance || type2 == TextClass.instance) {
            return TextClass.instance;
        }
        return null;
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        if (getType(type1, type2) != null) {
            TextClass type = TextClass.instance;
            if (type1 != type) {
                src1 = type.getCast(src1, compile.syntax, false);
            }
            if (type2 != type) {
                src2 = type.getCast(src2, compile.syntax, false);
            }
            return "(" + src1 + " || " + src2 + ")";
        }
        return null;
    }
}
