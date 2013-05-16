package platform.server.data.expr;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.formula.FormulaImpl;
import platform.server.data.expr.formula.StringAggConcatenateFormulaImpl;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.form.entity.drilldown.StringAggUnionDrillDownFormEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.FormulaUnionProperty;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

public class StringAggUnionProperty extends FormulaUnionProperty {

    private final String separator;
    private final ImList<CalcPropertyInterfaceImplement<Interface>> operands;

    public StringAggUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, String separator) {
        super(sID, caption, interfaces);
        this.separator = separator;
        this.operands = operands;

        finalizeInit();
    }

    @IdentityLazy
    protected FormulaImpl getFormula() {
        return new StringAggConcatenateFormulaImpl(separator);
    }

    @Override
    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new StringAggUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.agg.union"), this, BL
        );
    }
}
