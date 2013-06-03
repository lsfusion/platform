package lsfusion.server.data.expr.formula;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.server.classes.*;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

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
        if (exprType instanceof StringClass && ((StringClass)exprType).blankPadded) {
            exprSource = "rtrim(" + exprSource + ")";
        } else {
            exprSource = selfType.getCast(exprSource, compile.syntax, compile.env);
        }
        return exprSource;
    }

    @Override
    public StringClass getType(ExprSource source, KeyType keyType) {

        int separatorLength = separator.length();

        ExtInt length = ExtInt.ZERO;
        boolean caseInsensitive = false;
        boolean blankPadded = true;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = keyType == null ? source.getSelfType(i) : source.getType(i, keyType);

            length = length.sum(exprType != null ? exprType.getCharLength() : ExtInt.ZERO);
            if (exprType instanceof StringClass) {
                caseInsensitive = caseInsensitive || ((StringClass) exprType).caseInsensitive;
                blankPadded = blankPadded && ((StringClass) exprType).blankPadded;
            }

            if (i > 0) {
                length = length.sum(new ExtInt(separatorLength));
            }
        }

        if (forceCaseInsensitivity != null) {
            caseInsensitive = forceCaseInsensitivity;
        }

        return StringClass.get(blankPadded, caseInsensitive, length);
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
