package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class StringAggConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaUnionImpl {

    public StringAggConcatenateFormulaImpl(String separator) {
        super(separator, null);
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

    //считает, что последний expr - сепаратор
    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        assert exprCount > 1;
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
