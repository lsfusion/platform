package lsfusion.server.logics.property.classes.data;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.*;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.IncrementUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.XorUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.Function;

public class XorUnionProperty extends IncrementUnionProperty {

    public XorUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<PropertyInterfaceImplement<Interface>> operands) {
        super(caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final ImList<PropertyInterfaceImplement<Interface>> operands; // list нужен чтобы порядок редактирования был

    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImList<Expr> operandExprs = operands.mapListValues((PropertyInterfaceImplement<Interface> value) -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));

        Where xorWhere = Where.FALSE();
        for(Expr operandExpr : operandExprs)
            xorWhere = xorWhere.xor(operandExpr.getWhere());
        return ValueExpr.get(xorWhere);
    }

    @Override
    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // до непосредственно вычисления, для хинтов
        ImList<Pair<Expr, Where>> operandExprs = operands.mapListValues((Function<PropertyInterfaceImplement<Interface>, Pair<Expr, Where>>) key -> {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            return new Pair<>(key.mapExpr(joinImplement, propChanges, changedOperandWhere), changedOperandWhere.toWhere());
        });

        Where resultWhere = prevExpr.getWhere();
        for(int i=0,size=operands.size();i<size;i++) {
            PropertyInterfaceImplement<Interface> operand = operands.get(i);
            Pair<Expr, Where> operandExpr = operandExprs.get(i);
            Where prevOperandWhere = operand.mapExpr(joinImplement).getWhere();
            resultWhere = resultWhere.xor(operandExpr.first.getWhere().xor(prevOperandWhere).and(operandExpr.second));
            if(changedWhere!=null) changedWhere.add(operandExpr.second);
        }
        return ValueExpr.get(resultWhere);
    }

    @Override
    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges, CalcDataType type) {
        return SetFact.add(propChanges.getUsedDataChanges(type, getDepends()), propChanges.getUsedChanges(getDepends()));
    }

    @Override
    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        for(PropertyInterfaceImplement<Interface> operand : operands)
            result.addAll(operand.mapChangeProps());
        return result.immutable();
    }

    @Override
    public boolean canBeHeurChanged(boolean global) {
        for(PropertyInterfaceImplement<Interface> operand : operands) // считаем where сиблингов и потом ими xor'им change
            if(operand instanceof PropertyMapImplement && ((PropertyMapImplement) operand).property.canBeHeurChanged(global))
                return true;
        return false;
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, CalcDataType type, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(PropertyInterfaceImplement<Interface> operand : operands.reverseList()) {
            Where siblingWhere = Where.FALSE();
            for(PropertyInterfaceImplement<Interface> siblingOperand : operands) // считаем where сиблингов и потом ими xor'им change
                if(siblingOperand!=operand)
                    siblingWhere = siblingWhere.xor(siblingOperand.mapExpr(change.getMapExprs(), propChanges).getWhere());
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapJoinDataChanges(new PropertyChange<>(change, ValueExpr.get(change.expr.getWhere().xor(siblingWhere))), type, GroupType.ASSERTSINGLE_CHANGE(), operandWhere, propChanges));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return true;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new XorUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.xor.union}"), this, LM
        );
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.removeValues(super.calcInferValueClass(inferred, inferType));
    }
}
