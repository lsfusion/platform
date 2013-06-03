package lsfusion.server.logics.property;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.SumUnionDrillDownFormEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.base.BaseUtils.capitalize;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class SumUnionProperty extends IncrementUnionProperty {

    public SumUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImMap<CalcPropertyInterfaceImplement<Interface>, Integer> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final ImMap<CalcPropertyInterfaceImplement<Interface>,Integer> operands;

    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands.keys();
    }

    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImCol<Pair<Expr, Integer>> operandExprs = operands.mapColValues(new GetKeyValue<Pair<Expr, Integer>, CalcPropertyInterfaceImplement<Interface>, Integer>() { // до непосредственно вычисления, для хинтов
            public Pair<Expr, Integer> getMapValue(CalcPropertyInterfaceImplement<Interface> key, Integer value) {
                return new Pair<Expr, Integer>(key.mapExpr(joinImplement, propClasses, propChanges, changedWhere), value);
            }});

        Expr result = Expr.NULL;
        for(Pair<Expr, Integer> operandExpr : operandExprs)
            result = result.sum(operandExpr.first.scale(operandExpr.second));
        return result;
    }

    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, final WhereBuilder changedWhere) {
        ImMap<CalcPropertyInterfaceImplement<Interface>, Pair<Expr, Where>> operandExprs = operands.keys().mapValues(new GetValue<Pair<Expr, Where>, CalcPropertyInterfaceImplement<Interface>>() { // до непосредственно вычисления, для хинтов
            public Pair<Expr, Where> getMapValue(CalcPropertyInterfaceImplement<Interface> key) {
                WhereBuilder changedOperandWhere = new WhereBuilder();
                return new Pair<Expr, Where>(key.mapExpr(joinImplement, propChanges, changedOperandWhere), changedOperandWhere.toWhere());
            }
        });

        Expr result = prevExpr;
        for(int i=0,size=operands.size();i<size;i++) {
            CalcPropertyInterfaceImplement<Interface> operand = operands.getKey(i);
            Pair<Expr, Where> newOperandExpr = operandExprs.get(operand);
            Expr prevOperandExpr = operand.mapExpr(joinImplement);
            result = result.sum(newOperandExpr.first.diff(prevOperandExpr).and(newOperandExpr.second).scale(operands.getValue(i)));
            if(changedWhere!=null) changedWhere.add(newOperandExpr.second);
        }
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new SumUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.sum.union"), this, BL
        );
    }
}
