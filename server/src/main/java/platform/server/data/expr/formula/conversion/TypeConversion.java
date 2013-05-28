package platform.server.data.expr.formula.conversion;

import platform.server.data.type.Type;

public interface TypeConversion {
    Type getType(Type type1, Type type2);
}
