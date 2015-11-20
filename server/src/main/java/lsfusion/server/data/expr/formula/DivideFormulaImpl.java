package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.expr.formula.conversion.IntegralTypeConversion;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class DivideFormulaImpl extends ScaleFormulaImpl {

    public DivideFormulaImpl() {
        super(DivideTypeConversion.instance, DivideConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "division";
    }

    private static class DivideTypeConversion extends IntegralTypeConversion {
        public final static DivideTypeConversion instance = new DivideTypeConversion();

        @Override
        public IntegralClass getIntegralClass(IntegralClass type1, IntegralClass type2) {
            return type1.getDivide(type2);
        }
    }

    private static class DivideConversionSource extends ScaleConversionSource {
        public final static DivideConversionSource instance = new DivideConversionSource();

        private DivideConversionSource() {
            super(DivideTypeConversion.instance);
        }

        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                String source;
                if(Settings.get().isUseSafeDivision() && !isToString) {
                    source = "(" + src1 + "/" + syntax.getNotZero(src2, type, env) + ")";
                } else {
                    source = "(" + src1 + "/" + src2 + ")";
                }
                return getScaleSource(source, type, syntax, env, isToString);
            }
            return null;
        }
    }

    public boolean hasNotNull() {
        return super.hasNotNull() || Settings.get().isUseSafeDivision();
    }
}
