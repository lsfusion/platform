package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class SubtractFormulaImpl extends ArithmeticFormulaImpl {

    public SubtractFormulaImpl() {
        super(IntegralTypeConversion.sumTypeConversion, SubtractConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "subtract";
    }

    private static class SubtractConversionSource extends AbstractConversionSource {
        public final static SubtractConversionSource instance = new SubtractConversionSource();

        protected SubtractConversionSource() {
            super(IntegralTypeConversion.sumTypeConversion);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                return "(" + src1 + "-" + src2 + ")";
            }
            return null;
        }
    }
}
