package lsfusion.server.data.expr.formula;

import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.data.sql.SQLSyntax;

// obsolete - вместо SumFormulaImpl
public class StringJoinConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaJoinImpl {

    public StringJoinConcatenateFormulaImpl(String separator, Boolean forceCaseInsensitivity) {
        super(separator, forceCaseInsensitivity);
    }

    public String getSource(ExprSource source) {
        StringClass type = getType(source);
        SQLSyntax syntax = source.getSyntax();
        
        String separator = " " + syntax.getStringConcatenate() + " '" + this.separator + "' " + syntax.getStringConcatenate() + " ";
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            String exprSource = getExprSource(source, type, i);

            if (i > 0) {
                builder.append(separator);
            }

            builder.append(exprSource);
        }
        builder.append(")");
        return type.getCast(builder.toString(), syntax, source.getMEnv());
    }

    public boolean hasNotNull() {
        return false;
    }
}
