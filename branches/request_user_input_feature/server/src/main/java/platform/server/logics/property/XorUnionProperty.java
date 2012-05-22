package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

public class XorUnionProperty extends IncrementUnionProperty {

    public XorUnionProperty(String sID, String caption, List<Interface> interfaces, List<CalcPropertyInterfaceImplement<Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final List<CalcPropertyInterfaceImplement<Interface>> operands; // list нужен чтобы порядок редактирования был

    protected Collection<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands;
    }

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where xorWhere = Where.FALSE;
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            xorWhere = xorWhere.xor(operand.mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere());
        return ValueExpr.get(xorWhere);
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Where resultWhere = prevExpr.getWhere();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands) {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            Where newOperandWhere = operand.mapExpr(joinImplement, propChanges, changedOperandWhere).getWhere();
            Where prevOperandWhere = operand.mapExpr(joinImplement).getWhere();
            resultWhere = resultWhere.xor(newOperandWhere.xor(prevOperandWhere).and(changedOperandWhere.toWhere()));
            if(changedWhere!=null) changedWhere.add(changedOperandWhere.toWhere());
        }
        return ValueExpr.get(resultWhere);
    }

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return QuickSet.add(propChanges.getUsedDataChanges(getDepends()), propChanges.getUsedChanges(getDepends()));
    }

    @Override
    @IdentityLazy
    public Collection<DataProperty> getChangeProps() {
        Set<DataProperty> result = new HashSet<DataProperty>();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            result.addAll(operand.mapChangeProps());
        return result;
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = new DataChanges();
        for(CalcPropertyInterfaceImplement<Interface> operand : BaseUtils.reverse(operands)) {
            Where siblingWhere = Where.FALSE;
            for(CalcPropertyInterfaceImplement<Interface> siblingOperand : operands) // считаем where сиблингов и потом ими xor'им change
                if(siblingOperand!=operand)
                    siblingWhere = siblingWhere.xor(siblingOperand.mapExpr(change.getMapExprs(), propChanges).getWhere());
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapDataChanges(new PropertyChange<Interface>(change, ValueExpr.get(change.expr.getWhere().xor(siblingWhere))), operandWhere, propChanges));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }
}
