package platform.server.data.expr.formula.conversion;

import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public interface ConversionSource {
    String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2);
}
