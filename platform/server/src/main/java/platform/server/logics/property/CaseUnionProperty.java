package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.util.*;

public class CaseUnionProperty extends UnionProperty {

    private class Case {
        PropertyMapImplement<?, Interface> where;
        PropertyMapImplement<?, Interface> property;

        private Case(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
            this.where = where;
            this.property = property;
        }
    }
    private List<Case> cases = new ArrayList<Case>();
    public void addCase(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
        cases.add(new Case(where, property));
    }

    public CaseUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases)
            exprCases.add(propCase.where.mapExpr(joinImplement, modifier, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, modifier, changedWhere));
        return exprCases.getFinal();
    }

    @Override
    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return BaseUtils.merge(getWheres(), getProps());
    }

    private Collection<PropertyMapImplement<?, Interface>> getWheres() {
        List<PropertyMapImplement<?, Interface>> operands = new ArrayList<PropertyMapImplement<?,Interface>>();
        for(Case propCase : cases)
            operands.add(propCase.where);
        return operands;
    }
    private Collection<PropertyMapImplement<?, Interface>> getProps() {
        List<PropertyMapImplement<?, Interface>> operands = new ArrayList<PropertyMapImplement<?,Interface>>();
        for(Case propCase : cases)
            operands.add(propCase.where);
        return operands;
    }

    @Override
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        Set<Property> propValues = new HashSet<Property>(); fillDepends(propValues, getProps());
        Set<Property> propWheres = new HashSet<Property>(); fillDepends(propWheres, getWheres());
        return modifier.getUsedDataChanges(propValues).add(modifier.getUsedChanges(propValues));
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(Case operand : cases) {
            Where caseWhere = operand.where.mapExpr(change.mapKeys, modifier).getWhere();

            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.property.mapDataChanges(change.and(caseWhere), operandWhere, modifier));
            change = change.and(caseWhere.not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }
}
