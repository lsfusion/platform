package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

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
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                if(Settings.get().isUseSafeDivision()) {
                    return "(" + src1 + "/" + syntax.getNotZero(src2, type, env) + ")";
                } else {
                    return "(" + src1 + "/" + src2 + ")";
                }
            }
            return null;
        }
    }

    public boolean hasNotNull() {
        return Settings.get().isUseSafeDivision();
    }
}
