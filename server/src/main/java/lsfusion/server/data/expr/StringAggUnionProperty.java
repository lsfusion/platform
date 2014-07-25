package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.data.expr.formula.FormulaUnionImpl;
import lsfusion.server.data.expr.formula.StringAggConcatenateFormulaImpl;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.StringAggUnionDrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.FormulaUnionProperty;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class StringAggUnionProperty extends FormulaUnionProperty {

    private final String separator;
    private final ImList<CalcPropertyInterfaceImplement<Interface>> operands;

    public StringAggUnionProperty(String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, String separator) {
        super(caption, interfaces);
        this.separator = separator;
        this.operands = operands;

        finalizeInit();
    }

    @IdentityLazy
    protected FormulaUnionImpl getFormula() {
        return new StringAggConcatenateFormulaImpl(separator);
    }

    @Override
    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new StringAggUnionDrillDownFormEntity(
                canonicalName, getString("logics.property.drilldown.form.agg.union"), this, LM
        );
    }
}
