package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.MaxUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class MaxUnionProperty extends IncrementFormulaUnionProperty {

    private final ImCol<PropertyInterfaceImplement<Interface>> operands;
    private boolean isMin;

    @Override
    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    public Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {

        // до непосредственно вычисления, для хинтов
        ImCol<Expr> operandExprs = operands.mapColValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));

        Expr result = null;
        for(Expr operandExpr : operandExprs) {
            if(result==null)
                result = operandExpr;
            else if (isMin)
                result = result.min(operandExpr);
            else
                result = result.max(operandExpr);
        }
        return result;
    }

    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return operands;
    }

    public MaxUnionProperty(boolean isMin, LocalizedString caption, ImOrderSet<Interface> interfaces, ImCol<PropertyInterfaceImplement<Interface>> operands) {
        super(caption, interfaces);
        this.operands = operands;
        this.isMin = isMin;

        finalizeInit();
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BaseLogicsModule LM) {
        return new MaxUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.max.union}"), this, LM
        );
    }
}
