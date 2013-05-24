package platform.server.data.expr.formula.conversion;

import platform.server.classes.StringClass;
import platform.server.classes.TextClass;
import platform.server.classes.VarStringClass;
import platform.server.data.type.Type;

import static platform.server.classes.StringClass.get;

public class StringTypeConversion implements TypeConversion {
    public final static StringTypeConversion instance = new StringTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 == TextClass.instance || type2 == TextClass.instance) {
            return TextClass.instance;
        }
        if (type1 instanceof StringClass || type2 instanceof StringClass) {
            int length1 = type1 == null ? 0 : type1.getCharLength();
            int length2 = type2 == null ? 0 : type2.getCharLength();

            boolean caseInsensitive =
                    (type1 instanceof StringClass && ((StringClass) type1).caseInsensitive) ||
                            (type2 instanceof StringClass && ((StringClass) type2).caseInsensitive);

            boolean isVar = type1 instanceof VarStringClass || type2 instanceof VarStringClass;

            return get(isVar, caseInsensitive, length1 + length2);
        }
        return null;
    }
}
