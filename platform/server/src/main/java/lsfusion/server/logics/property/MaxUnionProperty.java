package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.MaxUnionDrillDownFormEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.base.BaseUtils.capitalize;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class MaxUnionProperty extends IncrementUnionProperty {

    private final ImCol<CalcPropertyInterfaceImplement<Interface>> operands;
    private boolean isMin;

    @Override
    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    public Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {

        ImCol<Expr> operandExprs = operands.mapColValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() { // до непосредственно вычисления, для хинтов
            public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});

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

    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands;
    }

    public MaxUnionProperty(boolean isMin, String sID, String caption, ImOrderSet<Interface> interfaces, ImCol<CalcPropertyInterfaceImplement<Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;
        this.isMin = isMin;

        finalizeInit();
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new MaxUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.max.union"), this, BL
        );
    }
}
