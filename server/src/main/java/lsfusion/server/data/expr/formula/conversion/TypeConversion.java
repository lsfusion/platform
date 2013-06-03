package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.type.Type;

public interface TypeConversion {
    Type getType(Type type1, Type type2);
}
