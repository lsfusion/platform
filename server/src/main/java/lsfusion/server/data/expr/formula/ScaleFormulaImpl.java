package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.formula.conversion.AbstractConversionSource;
import lsfusion.server.data.expr.formula.conversion.IntegralTypeConversion;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;

public abstract class ScaleFormulaImpl extends ArithmeticFormulaImpl {

    public ScaleFormulaImpl(IntegralTypeConversion typeConversion, ScaleConversionSource conversionSource) {
        super(typeConversion, conversionSource);
    }

    protected static abstract class ScaleConversionSource extends AbstractConversionSource {

        public ScaleConversionSource(IntegralTypeConversion typeConversion) {
            super(typeConversion);
        }
    }
}
