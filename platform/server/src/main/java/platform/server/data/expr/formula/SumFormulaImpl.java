package platform.server.data.expr.formula;

import platform.server.classes.AbstractStringClass;
import platform.server.data.expr.formula.conversion.*;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

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
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
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
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                if (!(type1 instanceof AbstractStringClass)) {
                    src1 = type.getCast(src1, compile.syntax, compile.env, false);
                } else if (((AbstractStringClass)type1).needRTrim()) {
                    src1 = "rtrim(" + src1 + ")";
                }

                if (!(type2 instanceof AbstractStringClass)) {
                    src2 = type.getCast(src2, compile.syntax, compile.env, false);
                } else if (((AbstractStringClass)type2).needRTrim()) {
                    src2 = "rtrim(" + src2 + ")";
                }

                return type.getCast("(" + src1 + " || " + src2 + ")", compile.syntax, compile.env, false);
            }
            return null;
        }
    }
}
