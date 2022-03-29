package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImMap;

public class UnionCustomFormulaImpl extends CustomFormulaImpl implements FormulaUnionImpl {

    public UnionCustomFormulaImpl(CustomFormulaSyntax formula, ImMap<String, Integer> mapParams, FormulaClass valueClass) {
        super(formula, mapParams, valueClass);
    }

    @Override
    public boolean supportRemoveNull() {
        return false;
    }

    @Override
    public boolean supportSingleSimplify() {
        return false;
    }

    @Override
    public boolean supportNeedValue() {
        return false;
    }

    @Override
    public int hashCode() {
        return (super.hashCode()) + 1;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (this == o || o instanceof UnionCustomFormulaImpl);
    }
}
