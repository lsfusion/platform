package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.formula.conversion.IntegralTypeConversion;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;

public class MultiplyFormulaImpl extends ScaleFormulaImpl {
    @Override
    public String getOperationName() {
        return "multiplication";
    }

    private static class MultiplyTypeConversion extends IntegralTypeConversion {
        public final static MultiplyTypeConversion instance = new MultiplyTypeConversion();

        public IntegralClass getIntegralClass(IntegralClass type1, IntegralClass type2) {
            return type1.getMultiply(type2);
        }
    }

    private static class MultiplyConversionSource extends ScaleConversionSource {
        public final static MultiplyConversionSource instance = new MultiplyConversionSource();

        private MultiplyConversionSource() {
            super(MultiplyTypeConversion.instance);
        }

        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            if(isToString)
                return "(" + src1 + "*" + src2 + ")";

            Type type = conversion.getType(type1, type2);
            if (type != null) {
                return type.getArithCast("(" + src1 + "*" + src2 + ")", syntax, env); // since type can be limited by max values + double / int / long
            }
            return null;
        }
    }

    @Override
    public boolean hasNotNull(ImList<BaseExpr> exprs) {
        return true;
    }

    public final static MultiplyFormulaImpl instance = new MultiplyFormulaImpl();

    private MultiplyFormulaImpl() { // private - no equals / hashcode
        super(MultiplyTypeConversion.instance, MultiplyConversionSource.instance);
    }
}
