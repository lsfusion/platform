package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class SumFormulaImpl extends ArithmeticFormulaImpl {
    public final static CompoundTypeConversion sumConversion = new CompoundTypeConversion(
            StringTypeConversion.instance,
            IntegralTypeConversion.instance
    );

    public final static CompoundConversionSource sumConversionSource = new CompoundConversionSource(
            StringSumConversionSource.instance,
            IntegralSumConversionSource.instance
    );

    public SumFormulaImpl() {
        super(sumConversion, sumConversionSource);
    }

    @Override
    public String getOperationName() {
        return "sum";
    }

    public static class IntegralSumConversionSource extends AbstractConversionSource {
        public final static IntegralSumConversionSource instance = new IntegralSumConversionSource();

        protected IntegralSumConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, ExecuteEnvironment env) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                return "(" + src1 + "+" + src2 + ")";
            }
            return null;
        }
    }

    public static class StringSumConversionSource extends AbstractConversionSource {
        public final static StringSumConversionSource instance = new StringSumConversionSource();

        protected StringSumConversionSource() {
            super(StringTypeConversion.instance);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, ExecuteEnvironment env) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                if (!(type1 instanceof StringClass)) {
                    src1 = type.getCast(src1, syntax, env);
                } else if (((StringClass)type1).blankPadded) {
                    src1 = "rtrim(" + src1 + ")";
                }

                if (!(type2 instanceof StringClass)) {
                    src2 = type.getCast(src2, syntax, env);
                } else if (((StringClass)type2).blankPadded) {
                    src2 = "rtrim(" + src2 + ")";
                }

                return type.getCast("(" + src1 + " || " + src2 + ")", syntax, env);
            }
            return null;
        }
    }
}
