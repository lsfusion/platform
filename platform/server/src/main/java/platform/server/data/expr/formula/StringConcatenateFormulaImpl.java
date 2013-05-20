package platform.server.data.expr.formula;

import platform.base.BaseUtils;
import platform.server.classes.*;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class StringConcatenateFormulaImpl extends AbstractFormulaImpl {
    protected final String separator;
    protected final Boolean forceCaseInsensitivity;

    public StringConcatenateFormulaImpl(String separator, Boolean forceCaseInsensitivity) {
        this.separator = separator;
        this.forceCaseInsensitivity = forceCaseInsensitivity;
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
        boolean caseInsensitive = false;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = keyType == null ? source.getSelfType(i) : source.getType(i, keyType);

            //конеченый тип будет Text, если тип одного из операторов будет Text
            //иначе тип будет VarString, если тип одного из операторов будет VarString
            //иначе тип будет String
            //тип будет caseInsensitive, если тип одного из операторов - caseInsensitive
            if (exprType instanceof TextClass) {
                isText = true;
                break;
            } else {
                length += exprType != null ? exprType.getBinaryLength(true) : 0;
                if (exprType instanceof StringClass) {
                    caseInsensitive = caseInsensitive || ((StringClass) exprType).caseInsensitive;
                    if (exprType instanceof VarStringClass) {
                        isVar = true;
                    }
                }
            }

            if (i > 0) {
                length += separatorLength;
            }
        }

        if (forceCaseInsensitivity != null) {
            caseInsensitive = forceCaseInsensitivity;
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
        return BaseUtils.nullEquals(forceCaseInsensitivity,that.forceCaseInsensitivity) && separator.equals(that.separator);
    }

    @Override
    public int hashCode() {
        int result = separator.hashCode();
        result = 31 * result + (forceCaseInsensitivity == null ? 0 : forceCaseInsensitivity ? 1 : 2);
        return result;
    }
}
