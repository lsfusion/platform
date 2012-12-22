package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

// вообще
public class IfUnionProperty extends IncrementUnionProperty {

    private CalcPropertyInterfaceImplement<Interface> ifProp;
    private CalcPropertyInterfaceImplement<Interface> trueProp;
    private CalcPropertyInterfaceImplement<Interface> falseProp;

    protected ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return SetFact.toSet(ifProp, trueProp, falseProp);
    }

    @Override
    @IdentityLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        ActionPropertyMapImplement<?, Interface> result = falseProp.mapEditAction(editActionSID, filterProperty);
        ActionPropertyMapImplement<?, Interface> trueAction = trueProp.mapEditAction(editActionSID, filterProperty);
        if (trueAction != null) {
            result = DerivedProperty.createIfAction(interfaces, (CalcPropertyMapImplement<?, Interface>) ifProp, trueAction, result);
        }
        return result;
    }

    @Override
    @IdentityLazy
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        result.addAll(trueProp.mapChangeProps());
        result.addAll(falseProp.mapChangeProps());
        return result.immutable();
    }

    public IfUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, CalcPropertyInterfaceImplement<Interface> ifProp, CalcPropertyInterfaceImplement<Interface> trueProp, CalcPropertyInterfaceImplement<Interface> falseProp) {
        super(sID, caption, interfaces);
        this.ifProp = ifProp;
        this.trueProp = trueProp;
        this.falseProp = falseProp;

        finalizeInit();
    }

    protected Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return trueProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere).ifElse(ifProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere(), falseProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere));
    }

    protected Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        WhereBuilder changedIf = new WhereBuilder();
        Where caseWhere = ifProp.mapExpr(joinImplement, propChanges, changedIf).getWhere();

        WhereBuilder changedTrue = new WhereBuilder();
        Expr trueExpr = trueProp.mapExpr(joinImplement, propChanges, changedTrue);

        Where changedIfTrue = caseWhere.and(changedIf.toWhere().or(changedTrue.toWhere()));

        WhereBuilder changedFalse = new WhereBuilder();
        Expr falseExpr = falseProp.mapExpr(joinImplement, propChanges, changedFalse);

        Where changedIfFalse = caseWhere.not().and(changedIf.toWhere().or(changedFalse.toWhere()));
        
        Where changedOrWhere = changedIfTrue.exclOr(changedIfFalse);
        if(changedWhere!=null) changedWhere.add(changedOrWhere);

        CaseExprInterface exprCases = Expr.newCases(true);
        exprCases.add(changedIfTrue, trueExpr);
        exprCases.add(changedIfFalse, falseExpr);
        exprCases.add(changedOrWhere, prevExpr);

        return exprCases.getFinal();
    }

    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        MSet<CalcProperty> mPropValues = SetFact.mSet(); trueProp.mapFillDepends(mPropValues); falseProp.mapFillDepends(mPropValues);
        MSet<CalcProperty> mPropWheres = SetFact.mSet(); ifProp.mapFillDepends(mPropWheres);
        return SetFact.add(propChanges.getUsedDataChanges(mPropValues.immutable()), propChanges.getUsedChanges(mPropWheres.immutable()));
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        Where ifWhere = ifProp.mapExpr(change.getMapExprs(), propChanges).getWhere();
        return trueProp.mapDataChanges(change.and(ifWhere), changedWhere, propChanges).add(
                falseProp.mapDataChanges(change.and(ifWhere.not()), changedWhere, propChanges));
    }
}
