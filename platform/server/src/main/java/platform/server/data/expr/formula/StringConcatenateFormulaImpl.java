package platform.server.data.expr.formula;

import platform.server.classes.*;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class StringConcatenateFormulaImpl extends AbstractFormulaImpl {
    protected final String separator;
    protected final boolean caseInsensitive;

    public StringConcatenateFormulaImpl(String separator, boolean caseInsensitive) {
        this.separator = separator;
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        Type type = getType(source, compile.keyType);

        String separator = " || '" + this.separator + "' || ";
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            String exprSource = getExprSource(compile, source, type, i);

            if (i > 0) {
                builder.append(separator);
            }

            builder.append(exprSource);
        }
        builder.append(")");
        return builder.toString();
    }

    protected String getExprSource(CompileSource compile, ExprSource source, Type selfType, int i) {
        Type exprType = source.getType(i, compile.keyType);
        String exprSource = source.getSource(i, compile);
        if (exprType instanceof StringClass && !(exprType instanceof VarStringClass)) {
            exprSource = "rtrim(" + exprSource + ")";
        } else {
            exprSource = selfType.getCast(exprSource, compile.syntax, false);
        }
        return exprSource;
    }

    @Override
    public AbstractStringClass getType(ExprSource source, KeyType keyType) {

        int separatorLength = separator.length();

        int length = 0;
        boolean isVar = false;
        boolean isText = false;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = keyType == null ? source.getSelfType(i) : source.getType(i, keyType);

            //конеченый тип будет Text, если тип одного из операторов будет Text
            //иначе тип будет соответсвовать типу последнего оператор типа String или VarString
            if (exprType instanceof TextClass) {
                isText = true;
                break;
            } else {
                length += exprType != null ? exprType.getBinaryLength(true) : 0;
                if (exprType instanceof StringClass) {
                    isVar = exprType instanceof VarStringClass;
                }
            }

            if (i > 0) {
                length += separatorLength;
            }
        }

        return isText ? TextClass.instance : StringClass.get(isVar, caseInsensitive, length);
    }

    @Override
    public ConcreteClass getStaticClass(ExprSource source) {
        return getType(source, null);
    }

    @Override
    public boolean equals(Object o) {
        StringConcatenateFormulaImpl that = (StringConcatenateFormulaImpl) o;
        return caseInsensitive == that.caseInsensitive && separator.equals(that.separator);
    }

    @Override
    public int hashCode() {
        int result = separator.hashCode();
        result = 31 * result + (caseInsensitive ? 1 : 0);
        return result;
    }
}
