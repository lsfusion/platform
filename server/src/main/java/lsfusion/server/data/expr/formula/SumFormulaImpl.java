package lsfusion.server.data.expr.formula;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;

public class SumFormulaImpl extends ArithmeticFormulaImpl {
    public final static CompoundTypeConversion sumConversion = new CompoundTypeConversion(
            StringTypeConversion.instance,
            IntegralTypeConversion.instance,
            TimeSeriesTypeConversion.instance
    );

    public final static CompoundConversionSource sumConversionSource = new CompoundConversionSource(
            StringConversionSource.instance,
            IntegralConversionSource.instance,
            TimeSeriesConversionSource.instance
    );

    public final static SumFormulaImpl instance = new SumFormulaImpl();

    private SumFormulaImpl() { // private - no equals / hashcode
        super(sumConversion, sumConversionSource);
    }

    @Override
    public String getOperationName() {
        return "sum";
    }

    public static class IntegralConversionSource extends AbstractConversionSource {
        public final static IntegralConversionSource instance = new IntegralConversionSource();

        protected IntegralConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                return "(" + src1 + "+" + src2 + ")"; // here also cast maybe should be used
            }
            return null;
        }
    }

    public static class StringTypeConversion implements TypeConversion {
        public final static StringTypeConversion instance = new StringTypeConversion();

        @Override
        public Type getType(Type type1, Type type2) {
            if (type1 instanceof StringClass || type2 instanceof StringClass) {
                if(type1 instanceof TextClass || type2 instanceof TextClass)
                    return (type1 instanceof RichTextClass) || (type2 instanceof RichTextClass) ? RichTextClass.instance :
                            (type1 instanceof HTMLTextClass) || (type2 instanceof HTMLTextClass) ? HTMLTextClass.instance : TextClass.instance;

                boolean caseInsensitive =
                        (type1 instanceof StringClass && ((StringClass) type1).caseInsensitive) ||
                                (type2 instanceof StringClass && ((StringClass) type2).caseInsensitive);

                boolean blankPadded =
                        (type1 instanceof StringClass && ((StringClass) type1).blankPadded) &&
                                (type2 instanceof StringClass && ((StringClass) type2).blankPadded);

                ExtInt length1 = type1 == null ? ExtInt.ZERO : type1.getCharLength();
                ExtInt length2 = type2 == null ? ExtInt.ZERO : type2.getCharLength();
                ExtInt length = length1.sum(length2);

                return StringClass.get(blankPadded, caseInsensitive, length);
            }
            return null;
        }
    }

    public static String castToVarString(String source, StringClass resultType, Type operandType, SQLSyntax syntax, TypeEnvironment typeEnv) {
        if(!(operandType instanceof StringClass) || syntax.doesNotTrimWhenSumStrings())
            source = resultType.toVar().getCast(source, syntax, typeEnv, operandType, Type.CastType.TOSTRING);
        return source;
    }

    public static class StringConversionSource extends AbstractConversionSource {
        public final static StringConversionSource instance = new StringConversionSource();

        protected StringConversionSource() {
            super(StringTypeConversion.instance);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            if(isToString)
                return "(" + src1 + "+" + src2 + ")";
            
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                StringClass stringClass = (StringClass) type;

                src1 = castToVarString(src1, stringClass, type1, syntax, env);
                src2 = castToVarString(src2, stringClass, type2, syntax, env);

                return type.getCast("(" + src1 + " " + syntax.getStringConcatenate() + " " + src2 + ")", syntax, env);
            }
            return null;
        }
    }

    public static class TimeSeriesTypeConversion implements TypeConversion {

        public final static TimeSeriesTypeConversion instance = new TimeSeriesTypeConversion();

        @Override
        public Type getType(Type type1, Type type2) {
            if (type2 instanceof TimeSeriesClass && ((TimeSeriesClass<?>) type2).hasInterval() && (type1 == null || type1 instanceof IntegralClass)) {
                return type2;
            }

            if (type1 instanceof TimeSeriesClass && ((TimeSeriesClass<?>) type1).hasInterval() && (type2 == null || type2 instanceof IntegralClass)) {
                return type1;
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
                return "(" + src1 + "+" + src2 + ")";

            TimeSeriesClass type = (TimeSeriesClass) conversion.getType(type1, type2);
            if (type != null) {
                if(type2 instanceof TimeSeriesClass) {
                    String tsrc = src1; src1 = src2; src2 = tsrc;
                }

                if(type instanceof DateClass)  // we could use interval, but we want to get date and not timestamp
                    return "(" + src1 + " + " + IntegerClass.instance.getCast(src2, syntax, env) + ")";
                else
                    return "(" + src1 + " + " + type.getIntervalStepString() + " * " + src2 + ")";
            }
            return null;
        }
    }

}
