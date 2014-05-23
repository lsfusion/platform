package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.data.type.Type;

public class StringAggConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaUnionImpl {

    public StringAggConcatenateFormulaImpl(String separator) {
        super(separator, null);
    }

    public boolean supportRemoveNull() {
        return true;
    }

    //считает, что последний expr - сепаратор
    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        if (exprCount == 0) {
            return "";
        }

        Type type = getType(source);

        String result = getExprSource(source, type, 0);

        for (int i = 1; i < exprCount; i++) {
            String exprSource = getExprSource(source, type, i);

            if(Settings.get().isUseSafeStringAgg()) {
                result = "CASE WHEN " + result + " IS NOT NULL" +
                        " THEN " + result + " " + source.getSyntax().getStringConcatenate() + " (CASE WHEN " + exprSource + " IS NOT NULL THEN '" + separator + "' " + source.getSyntax().getStringConcatenate() + " " + exprSource + " ELSE '' END)" +
                        " ELSE " + exprSource + " END";
            } else {
                result = source.getSyntax().getStringCFunc() + "(" + result + "," + exprSource + ",'" + separator + "')";
            }
        }
        return "(" + result + ")";
//        return result;
    }
}
