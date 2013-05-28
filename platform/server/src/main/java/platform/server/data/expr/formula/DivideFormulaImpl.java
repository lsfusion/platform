package platform.server.data.expr.formula;

import platform.server.data.expr.formula.conversion.*;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class DivideFormulaImpl extends ArithmeticFormulaImpl {

    public DivideFormulaImpl() {
        super(IntegralTypeConversion.instance, DivideConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "division";
    }

    private static class DivideConversionSource extends AbstractConversionSource {
        public final static DivideConversionSource instance = new DivideConversionSource();

        protected DivideConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        @Override
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                return "(" + src1 + "/" + src2 + ")";
            }
            return null;
        }
    }
}
