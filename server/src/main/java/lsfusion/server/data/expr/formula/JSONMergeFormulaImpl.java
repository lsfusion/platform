package lsfusion.server.data.expr.formula;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.physics.admin.Settings;

public class JSONMergeFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    public static JSONMergeFormulaImpl instance = new JSONMergeFormulaImpl();

    private JSONMergeFormulaImpl() {
    }

    public boolean supportRemoveNull() {
        return true;
    }

    public boolean supportSingleSimplify() {
        return true;
    }

    public boolean supportNeedValue() {
        return true;
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        if (exprCount <= 1) {
            return "";
        }
        String result = source.getSource(0);
        for (int i = 1; i < exprCount; i++) {
            result = "jsonb_recursive_merge(" + result + "::jsonb," + source.getSource(i) + "::jsonb)";
        }
        return JSONClass.instance.getCast("(" + result + ")", source.getSyntax(), source.getMEnv());
    }
}
