package lsfusion.server.data.expr.formula;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.physics.admin.Settings;

public class StringAggConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaUnionImpl {

    public StringAggConcatenateFormulaImpl(String separator) {
        super(separator, null);
    }

    public boolean supportRemoveNull() {
        return true;
    }

    public boolean supportSingleSimplify() {
        return false; // because it will break implicit cast'ing (CONCAT '', f(), 5 will return 5 when f() is null)
    }

    public boolean supportNeedValue() {
        return true;
    }

    //считает, что последний expr - сепаратор
    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        assert exprCount > 0;
        if (exprCount == 0) {
            return "";
        }

        StringClass type = getType(source);
        SQLSyntax syntax = source.getSyntax();

        String result = getExprSource(source, type, 0);

        for (int i = 1; i < exprCount; i++) {
            String exprSource = getExprSource(source, type, i);

            if(Settings.get().isUseSafeStringAgg()) {
                result = "CASE WHEN " + result + " IS NOT NULL" +
                        " THEN " + result + " " + syntax.getStringConcatenate() + " (CASE WHEN " + exprSource + " IS NOT NULL THEN '" + separator + "' " + syntax.getStringConcatenate() + " " + exprSource + " ELSE '' END)" +
                        " ELSE " + exprSource + " END";
            } else {
                result = syntax.getStringCFunc() + "(" + result + "," + exprSource + ",'" + separator + "')";
            }
        }
        return type.getCast("(" + result + ")", syntax, source.getMEnv());
//        return result;
    }
}
