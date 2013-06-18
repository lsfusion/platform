package lsfusion.server.data.expr.formula;

import lsfusion.server.data.type.Type;

public class MaxFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    public final boolean isMin;
    public final boolean notObjectType;

    public MaxFormulaImpl(boolean isMin) {
        this(isMin, false);
    }

    public MaxFormulaImpl(boolean isMin, boolean notObjectType) {
        this.isMin = isMin;
        this.notObjectType = notObjectType;
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        if (exprCount == 0) {
            return "";
        }

        Type type = getType(source);

        String result = type.getCast(source.getSource(0), source.getSyntax(), source.getEnv()); // чтобы когда NULL'ы тип правильно определило

        for (int i = 1; i < exprCount; i++)
            result = (isMin ? "MIN" : "MAX") + "(" + result + "," + source.getSource(i) + ")";
        return result;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof MaxFormulaImpl && isMin == ((MaxFormulaImpl) o).isMin && notObjectType == ((MaxFormulaImpl) o).notObjectType;
    }

    public int hashCode() {
        return 31 * (isMin ? 1 : 0) + (notObjectType ? 1 : 0);
    }
}
