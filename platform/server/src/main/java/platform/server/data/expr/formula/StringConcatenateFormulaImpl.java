package platform.server.data.expr.formula;

import platform.server.classes.InsensitiveStringClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class StringConcatenateFormulaImpl extends AbstractFormulaImpl {
    private final String separator;
    private final boolean caseSensitive;

    public StringConcatenateFormulaImpl(String separator, boolean caseSensitive) {
        this.separator = separator;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        String delimeter = " || '" + separator + "' || ";
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = source.getType(i, compile.keyType);
            String exprSource = source.getSource(i, compile);
            if (exprType instanceof StringClass) {
                exprSource = "rtrim(" + exprSource + ")";
            }

            if (i != 0) {
                builder.append(delimeter);
            }

            builder.append(exprSource);
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public Type getType(ExprSource source, KeyType keyType) {
        int length = 0;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            length += source.getType(i, keyType).getBinaryLength(true);
        }
        return caseSensitive ? StringClass.get(length) : InsensitiveStringClass.get(length);
    }

    @Override
    public boolean equals(Object o) {
        StringConcatenateFormulaImpl that = (StringConcatenateFormulaImpl) o;
        return caseSensitive == that.caseSensitive && separator.equals(that.separator);
    }

    @Override
    public int hashCode() {
        int result = separator.hashCode();
        result = 31 * result + (caseSensitive ? 1 : 0);
        return result;
    }
}
