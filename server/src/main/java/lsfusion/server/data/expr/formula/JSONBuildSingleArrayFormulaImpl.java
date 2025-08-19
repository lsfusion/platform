package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;

public class JSONBuildSingleArrayFormulaImpl implements FormulaJoinImpl {

    @Override
    public boolean hasNotNull(ImList<BaseExpr> exprs) {
        return false;
    }

    private JSONBuildSingleArrayFormulaImpl() {
    }

    public static final JSONBuildSingleArrayFormulaImpl instance = new JSONBuildSingleArrayFormulaImpl();

    @Override
    public String getSource(ExprSource source) {
        String valueSource = source.getSource(0);
//        Type type = source.getType(i);
//        if(type != null)
//            valueSource = type.formatJSONSource(valueSource, source.getSyntax());
        Type type = source.getType(0);
        return "CASE WHEN " + valueSource + " IS NOT NULL THEN "
                + (type instanceof JSONTextClass ? "json_build_array" : "jsonb_build_array")
                + "(" + valueSource + ") ELSE NULL END";
//        return "jsonb_build_array(" + valueSource + ")";
    }

    public Type getType(ExprType source) {
        Type type = source.getType(0);
        return type instanceof JSONTextClass ? JSONTextClass.instance : JSONClass.instance;
    }
}
