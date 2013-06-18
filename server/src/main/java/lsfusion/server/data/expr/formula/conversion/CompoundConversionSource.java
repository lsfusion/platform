package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

public class CompoundConversionSource implements ConversionSource {

    private final ConversionSource[] conversionSources;

    public CompoundConversionSource(ConversionSource... conversionSources) {
        this.conversionSources = conversionSources;
    }

    @Override
    public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, ExecuteEnvironment env) {
        String result = null;
        for (ConversionSource conversionSource : conversionSources) {
            result = conversionSource.getSource(type1, type2, src1, src2, syntax, env);
            if (result != null) {
                break;
            }
        }
        return result;
    }
}
