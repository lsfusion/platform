package lsfusion.server.data.expr.formula;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.server.classes.*;
import lsfusion.server.data.type.Type;

public abstract class StringConcatenateFormulaImpl extends AbstractFormulaImpl {
    protected final String separator;
    protected final Boolean forceCaseInsensitivity;

    public StringConcatenateFormulaImpl(String separator, Boolean forceCaseInsensitivity) {
        this.separator = separator;
        this.forceCaseInsensitivity = forceCaseInsensitivity;
    }

    protected String getExprSource(ExprSource source, Type selfType, int i) {
        Type exprType = source.getType(i);
        String exprSource = source.getSource(i);
        if (exprType instanceof StringClass && ((StringClass)exprType).blankPadded) {
            exprSource = ((StringClass)exprType).getRTrim(exprSource);
        } else {
            exprSource = selfType.getCast(exprSource, source.getSyntax(), source.getEnv(), exprType);
        }
        return exprSource;
    }

    @Override
    public Type getType(ExprType source) {

        int separatorLength = separator.length();

        ExtInt length = ExtInt.ZERO;
        boolean caseInsensitive = false;
        boolean blankPadded = true;
        boolean rich = false;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = source.getType(i);

            length = length.sum(exprType != null ? exprType.getCharLength() : ExtInt.ZERO);
            if (exprType instanceof StringClass) {
                StringClass strintType = (StringClass) exprType;
                caseInsensitive = caseInsensitive || strintType.caseInsensitive;
                blankPadded = blankPadded && strintType.blankPadded;
                rich = rich || strintType.rich;
            }

            if (i > 0) {
                length = length.sum(new ExtInt(separatorLength));
            }
        }

        if (forceCaseInsensitivity != null) {
            caseInsensitive = forceCaseInsensitivity;
        }

        return StringClass.get(blankPadded, caseInsensitive, rich, length);
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
