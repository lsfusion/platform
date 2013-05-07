package platform.server.data.expr.formula.conversion;

import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public interface TypeConversion {
    String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2);

    Type getType(Type type1, Type type2);
}
