package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImMap;

public class JoinCustomFormulaImpl extends CustomFormulaImpl implements FormulaJoinImpl {

    private final boolean hasNotNull;

    public boolean hasNotNull() {
        return hasNotNull;
    }

    public JoinCustomFormulaImpl(CustomFormulaSyntax formula, ImMap<String, Integer> mapParams, FormulaClass valueClass, boolean hasNotNull) {
        super(formula, mapParams, valueClass);

        this.hasNotNull = hasNotNull;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (hasNotNull ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (this == o || o instanceof JoinCustomFormulaImpl && hasNotNull==((JoinCustomFormulaImpl) o).hasNotNull);
    }
}
