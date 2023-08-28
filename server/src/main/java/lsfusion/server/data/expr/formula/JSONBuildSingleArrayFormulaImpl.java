package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;

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
        return "CASE WHEN " + valueSource + " IS NOT NULL THEN jsonb_build_array(" + valueSource + ") ELSE NULL END";
//        return "jsonb_build_array(" + valueSource + ")";
    }

    public Type getType(ExprType source) {
        return JSONClass.instance;
    }
}
