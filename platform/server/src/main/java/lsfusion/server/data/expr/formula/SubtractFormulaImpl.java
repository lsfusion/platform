package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public class SubtractFormulaImpl extends ArithmeticFormulaImpl {

    public SubtractFormulaImpl() {
        super(IntegralTypeConversion.instance, SubtractConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "subtract";
    }

    private static class SubtractConversionSource extends AbstractConversionSource {
        public final static SubtractConversionSource instance = new SubtractConversionSource();

        protected SubtractConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        @Override
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                return "(" + src1 + "-" + src2 + ")";
            }
            return null;
        }
    }
}
