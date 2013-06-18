package lsfusion.server.data.expr.formula;

import lsfusion.server.data.type.Type;

public class StringAggConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaUnionImpl {

    public StringAggConcatenateFormulaImpl(String separator) {
        super(separator, null);
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

            result = "CASE WHEN " + result + " IS NOT NULL" +
                    " THEN " + result + " || (CASE WHEN " + exprSource + " IS NOT NULL THEN '" + separator + "' || " + exprSource + " ELSE '' END)" +
                    " ELSE " + exprSource + " END";
        }
        return "(" + result + ")";
    }
}
