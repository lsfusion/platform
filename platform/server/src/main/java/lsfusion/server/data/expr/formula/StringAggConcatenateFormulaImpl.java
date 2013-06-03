package lsfusion.server.data.expr.formula;

import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public class StringAggConcatenateFormulaImpl extends StringConcatenateFormulaImpl {

    public StringAggConcatenateFormulaImpl(String separator) {
        super(separator, null);
    }

    //считает, что последний expr - сепаратор
    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        int exprCount = source.getExprCount();
        if (exprCount == 0) {
            return "";
        }

        Type type = getType(source, compile.keyType);

        String result = getExprSource(compile, source, type, 0);

        for (int i = 1; i < exprCount; i++) {
            String exprSource = getExprSource(compile, source, type, i);

            result = "CASE WHEN " + result + " IS NOT NULL" +
                    " THEN " + result + " || (CASE WHEN " + exprSource + " IS NOT NULL THEN '" + separator + "' || " + exprSource + " ELSE '' END)" +
                    " ELSE " + exprSource + " END";
        }
        return "(" + result + ")";
    }
}
