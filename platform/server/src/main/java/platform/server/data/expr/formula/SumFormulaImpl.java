package platform.server.data.expr.formula;

import platform.server.classes.StringClass;
import platform.server.classes.TextClass;
import platform.server.data.expr.formula.conversion.*;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class SumFormulaImpl extends ArithmeticFormulaImpl {
    public final static CompoundTypeConversion sumConversion = new CompoundTypeConversion(
            TextTypeConversion.instance,
            StringTypeConversion.instance,
            IntegralTypeConversion.instance
    );

    public final static CompoundConversionSource sumConversionSource = new CompoundConversionSource(
            TextSumConversionSource.instance,
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
                if (!(type1 instanceof StringClass)) {
                    src1 = type.getCast(src1, compile.syntax, false);
                }
                if (!(type2 instanceof StringClass)) {
                    src2 = type.getCast(src2, compile.syntax, false);
                }

                return "(rtrim(" + src1 + ") || rtrim(" + src2 + "))";
            }
            return null;
        }
    }

    public static class TextSumConversionSource extends AbstractConversionSource {
        public final static TextSumConversionSource instance = new TextSumConversionSource();

        protected TextSumConversionSource() {
            super(TextTypeConversion.instance);
        }

        @Override
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
            if (conversion.getType(type1, type2) != null) {
                TextClass type = TextClass.instance;
                if (type1 != type) {
                    src1 = type.getCast(src1, compile.syntax, false);
                }
                if (type2 != type) {
                    src2 = type.getCast(src2, compile.syntax, false);
                }
                return "(" + src1 + " || " + src2 + ")";
            }
            return null;
        }
    }
}
