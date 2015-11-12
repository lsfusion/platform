package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class MultiplyFormulaImpl extends ScaleFormulaImpl {

    public MultiplyFormulaImpl() {
        super(MultiplyConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "multiplication";
    }

    private static class MultiplyConversionSource extends ScaleConversionSource {
        public final static MultiplyConversionSource instance = new MultiplyConversionSource();

        public String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            Type type = conversion.getType(type1, type2);
            if (type != null || isToString) {
                return getScaleSource("(" + src1 + "*" + src2 + ")", type, syntax, env, isToString);
            }
            return null;
        }
    }
}
