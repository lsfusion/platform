package lsfusion.server.logics.property.classes.data;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.IncrementUnionProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.SumUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.Function;

public class SumUnionProperty extends IncrementUnionProperty {

    public SumUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImMap<PropertyInterfaceImplement<Interface>, Integer> operands) {
        super(caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final ImMap<PropertyInterfaceImplement<Interface>,Integer> operands;

    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return operands.keys();
    }

    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        // до непосредственно вычисления, для хинтов
        ImCol<Pair<Expr, Integer>> operandExprs = operands.mapColValues((key, value) -> new Pair<>(key.mapExpr(joinImplement, calcType, propChanges, changedWhere), value));

        Expr result = Expr.NULL();
        for(Pair<Expr, Integer> operandExpr : operandExprs)
            result = result.sum(operandExpr.first.scale(operandExpr.second));
        return result;
    }

    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, final WhereBuilder changedWhere) {
        // до непосредственно вычисления, для хинтов
        ImMap<PropertyInterfaceImplement<Interface>, Pair<Expr, Where>> operandExprs = operands.keys().mapValues((Function<PropertyInterfaceImplement<Interface>, Pair<Expr, Where>>) key -> {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            return new Pair<>(key.mapExpr(joinImplement, propChanges, changedOperandWhere), changedOperandWhere.toWhere());
        });

        Expr result = prevExpr;
        PropertyChanges prevPropChanges = getPrevPropChanges(propChanges);
        for(int i=0,size=operands.size();i<size;i++) {
            PropertyInterfaceImplement<Interface> operand = operands.getKey(i);
            Pair<Expr, Where> newOperandExpr = operandExprs.get(operand);
            Expr prevOperandExpr = operand.mapExpr(joinImplement, prevPropChanges);
            result = result.sum(newOperandExpr.first.diff(prevOperandExpr).and(newOperandExpr.second).scale(operands.getValue(i)));
            if(changedWhere!=null) changedWhere.add(newOperandExpr.second);
        }
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new SumUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.sum.union}"), this, LM
        );
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.removeValues(super.calcInferValueClass(inferred, inferType));
    }
}
