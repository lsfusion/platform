package lsfusion.server.data.expr.formula;

import lsfusion.server.data.type.Type;

// obsolete - вместо SumFormulaImpl
public class StringJoinConcatenateFormulaImpl extends StringConcatenateFormulaImpl implements FormulaJoinImpl {

    public StringJoinConcatenateFormulaImpl(String separator, Boolean forceCaseInsensitivity) {
        super(separator, forceCaseInsensitivity);
    }

    public String getSource(ExprSource source) {
        Type type = getType(source);

        String separator = " " + source.getSyntax().getStringConcatenate() + " '" + this.separator + "' " + source.getSyntax().getStringConcatenate() + " ";
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
        return builder.toString();
    }

    public boolean hasNotNull() {
        return false;
    }
}
