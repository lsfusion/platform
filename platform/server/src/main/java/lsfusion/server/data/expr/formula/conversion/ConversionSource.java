package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public interface ConversionSource {
    String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2);
}
