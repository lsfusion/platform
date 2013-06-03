package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public class CompoundConversionSource implements ConversionSource {

    private final ConversionSource[] conversionSources;

    public CompoundConversionSource(ConversionSource... conversionSources) {
        this.conversionSources = conversionSources;
    }

    @Override
    public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
        String result = null;
        for (ConversionSource conversionSource : conversionSources) {
            result = conversionSource.getSource(compile, type1, type2, src1, src2);
            if (result != null) {
                break;
            }
        }
        return result;
    }
}
