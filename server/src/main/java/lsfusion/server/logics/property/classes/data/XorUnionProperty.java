package lsfusion.server.logics.property.classes.data;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.property.IncrementUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.physics.admin.drilldown.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.XorUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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
        ImList<Expr> operandExprs = operands.mapListValues(new GetValue<Expr, PropertyInterfaceImplement<Interface>>() {
            public Expr getMapValue(PropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, calcType, propChanges, changedWhere);
            }});

        Where xorWhere = Where.FALSE;
        for(Expr operandExpr : operandExprs)
            xorWhere = xorWhere.xor(operandExpr.getWhere());
        return ValueExpr.get(xorWhere);
    }

    @Override
    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        ImList<Pair<Expr, Where>> operandExprs = operands.mapListValues(new GetValue<Pair<Expr, Where>, PropertyInterfaceImplement<Interface>>() { // до непосредственно вычисления, для хинтов
            public Pair<Expr, Where> getMapValue(PropertyInterfaceImplement<Interface> key) {
                WhereBuilder changedOperandWhere = new WhereBuilder();
                return new Pair<>(key.mapExpr(joinImplement, propChanges, changedOperandWhere), changedOperandWhere.toWhere());
            }
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
    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return SetFact.add(propChanges.getUsedDataChanges(getDepends()), propChanges.getUsedChanges(getDepends()));
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
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(PropertyInterfaceImplement<Interface> operand : operands.reverseList()) {
            Where siblingWhere = Where.FALSE;
            for(PropertyInterfaceImplement<Interface> siblingOperand : operands) // считаем where сиблингов и потом ими xor'им change
                if(siblingOperand!=operand)
                    siblingWhere = siblingWhere.xor(siblingOperand.mapExpr(change.getMapExprs(), propChanges).getWhere());
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapJoinDataChanges(new PropertyChange<>(change, ValueExpr.get(change.expr.getWhere().xor(siblingWhere))), GroupType.ASSERTSINGLE_CHANGE(), operandWhere, propChanges));
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
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new XorUnionDrillDownFormEntity(
                canonicalName, LocalizedString.create("{logics.property.drilldown.form.xor.union}"), this, LM
        );
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.removeValues(super.calcInferValueClass(inferred, inferType));
    }
}
