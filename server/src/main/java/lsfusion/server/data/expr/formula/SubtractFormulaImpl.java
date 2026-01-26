package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.HasTimeClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;

public class SubtractFormulaImpl extends ArithmeticFormulaImpl {

    public final static CompoundTypeConversion subtractConversion = new CompoundTypeConversion(
            IntegralTypeConversion.instance,
            TimeSeriesTypeConversion.instance
    );

    public final static CompoundConversionSource subtractConversionSource = new CompoundConversionSource(
            IntegralSubtractConversionSource.instance,
            TimeSeriesConversionSource.instance
    );

    public final static SubtractFormulaImpl instance = new SubtractFormulaImpl();

    private SubtractFormulaImpl() { // private - no equals / hashcode
        super(subtractConversion, subtractConversionSource);
    }

    @Override
    public String getOperationName() {
        return "subtract";
    }

    private static class IntegralSubtractConversionSource extends AbstractConversionSource {
        public final static IntegralSubtractConversionSource instance = new IntegralSubtractConversionSource();

        protected IntegralSubtractConversionSource() {
            super(IntegralTypeConversion.instance);
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

    public static class TimeSeriesTypeConversion implements TypeConversion {

        public final static TimeSeriesTypeConversion instance = new TimeSeriesTypeConversion();

        @Override
        public Type getType(Type type1, Type type2) {
            if (type1 instanceof TimeSeriesClass && ((TimeSeriesClass<?>) type1).hasInterval()) {
                if (type2 == null || type2 instanceof IntegralClass) {
                    return type1;
                }
                if (type2 instanceof DateClass) {
                    return IntegerClass.instance;
                }
                if (type2 instanceof TimeSeriesClass) {
                    return LongClass.instance;
                }
            }

            return null;
        }
    }

    public static class TimeSeriesConversionSource extends AbstractConversionSource {
        public final static TimeSeriesConversionSource instance = new TimeSeriesConversionSource();

        protected TimeSeriesConversionSource() {
            super(TimeSeriesTypeConversion.instance);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            if(isToString)
                return "(" + src1 + "-" + src2 + ")";

            Type type = conversion.getType(type1, type2);
            if (type != null) {
                if(type1 instanceof DateClass) {
                    if(type instanceof TimeSeriesClass) // we could use interval, but we want to get date and not timestamp
                        return "(" + src1 + " - " + IntegerClass.instance.getCast(src2, syntax, env) + ")";
                    else
                        return "(" + src1 + " - " + src2 + ")";
                } else {
                    HasTimeClass hasTimeClass = (HasTimeClass) type1;
                    if(type instanceof TimeSeriesClass)
                        return "(" + src1 + " - " + hasTimeClass.getIntervalStepString() + " * " + src2 + ")";
                    else
                        return type.getCast("extract(epoch from (" + src1 + " - " + src2 + ")) / extract(epoch from " + hasTimeClass.getIntervalStepString() + ")", syntax, env);
                }
            }
            return null;
        }
    }
}
