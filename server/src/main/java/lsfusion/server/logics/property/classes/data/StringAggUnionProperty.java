package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.formula.FormulaUnionImpl;
import lsfusion.server.data.expr.formula.StringAggConcatenateFormulaImpl;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.StringAggUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class StringAggUnionProperty extends FormulaUnionProperty {

    private final String separator;
    private final ImList<PropertyInterfaceImplement<Interface>> operands;

    public StringAggUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<PropertyInterfaceImplement<Interface>> operands, String separator) {
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
    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new StringAggUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.agg.union}"), this, LM
        );
    }
}
