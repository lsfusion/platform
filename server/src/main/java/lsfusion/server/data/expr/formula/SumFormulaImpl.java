package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.JSONClass;

public class SumFormulaImpl extends ArithmeticFormulaImpl {
    public final static CompoundTypeConversion sumConversion = new CompoundTypeConversion(
            StringTypeConversion.instance,
            IntegralTypeConversion.sumTypeConversion
    );

    public final static CompoundConversionSource sumConversionSource = new CompoundConversionSource(
            StringSumConversionSource.instance,
            IntegralSumConversionSource.instance
    );

    public final static SumFormulaImpl instance = new SumFormulaImpl();

    private SumFormulaImpl() { // private - no equals / hashcode
        super(sumConversion, sumConversionSource);
    }

    @Override
    public String getOperationName() {
        return "sum";
    }

    public static class IntegralSumConversionSource extends AbstractConversionSource {
        public final static IntegralSumConversionSource instance = new IntegralSumConversionSource();

        protected IntegralSumConversionSource() {
            super(IntegralTypeConversion.sumTypeConversion);
        }

        @Override
        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                return "(" + src1 + "+" + src2 + ")";
            }
            return null;
        }
    }
    
    public static String castToVarString(String source, StringClass resultType, Type operandType, SQLSyntax syntax, TypeEnvironment typeEnv) {
        if(!(operandType instanceof StringClass) || syntax.doesNotTrimWhenSumStrings())
            source = resultType.toVar().getCast(source, syntax, typeEnv, operandType);
        return source;
    }

    public static ImList<String> castToVarStrings(ImList<String> exprs, final ImList<? extends ClassReader> readers, final Type resultType, final SQLSyntax syntax, final TypeEnvironment typeEnv) {
        return exprs.mapListValues((i, value) -> {
            ClassReader reader = readers.get(i);
            if(reader instanceof Type) {
                if(resultType instanceof JSONClass)
                    value = resultType.getCast(value, syntax, typeEnv, (Type) reader);
                else
                    value = castToVarString(value, ((StringClass) resultType), (Type) reader, syntax, typeEnv);
            }
            return value;
        });
    }

    public static class StringSumConversionSource extends AbstractConversionSource {
        public final static StringSumConversionSource instance = new StringSumConversionSource();

        protected StringSumConversionSource() {
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
}
