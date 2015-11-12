package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.*;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class DivideFormulaImpl extends ScaleFormulaImpl {

    public DivideFormulaImpl() {
        super(DivideConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "division";
    }

    private static class DivideConversionSource extends ScaleConversionSource {
        public final static DivideConversionSource instance = new DivideConversionSource();

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
