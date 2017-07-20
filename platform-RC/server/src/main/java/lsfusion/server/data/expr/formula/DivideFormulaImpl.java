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

        private static boolean hasDivisionProblem(DataClass type, int scaleProblem) {
            return type != null && type instanceof IntegralClass && ((IntegralClass) type).getPrecision() > scaleProblem;
        }

        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                Settings settings = Settings.get();
                int scaleProblem;
                if(settings.isUseCastDivisionOperands() && (scaleProblem = syntax.getFloatingDivisionProblem()) >= 0) {
                    if(hasDivisionProblem(type1,scaleProblem)) // если может быть больше scaleProblem - в явную прокастим
                        src1 = type1.getCast(src1, syntax, env);

                    if(hasDivisionProblem(type2, scaleProblem))
                        src2 = type2.getCast(src2, syntax, env);
                }

                String source;
                if(settings.isUseSafeDivision() && !isToString) {
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
