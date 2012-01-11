package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

public class ExclusiveCaseUnionProperty extends AbstractCaseUnionProperty {

    private Set<Case> cases = new HashSet<Case>();
    public void addCase(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
        cases.add(new Case(where, property));
    }
    protected Collection<Case> getCases() {
        return cases;
    }

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases)
            exprCases.add(propCase.where.mapExpr(joinImplement, propChanges, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, propChanges, changedWhere));
        return exprCases.getFinal();
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Where nullWhere = Where.FALSE;
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases) {
            WhereBuilder changedWhereCase = new WhereBuilder();
            Where caseWhere = propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase).getWhere();

            WhereBuilder changedExprCase = new WhereBuilder();
            Expr caseExpr = propCase.property.mapExpr(joinImplement, propChanges, changedExprCase);

            Where changedCase = changedWhereCase.toWhere().or(changedExprCase.toWhere());
            if(changedWhere!=null) changedWhere.add(changedCase);
            exprCases.add(caseWhere.and(changedCase), caseExpr);
            nullWhere = nullWhere.or(changedWhereCase.toWhere().and(propCase.where.mapExpr(joinImplement).getWhere()));
        }
        exprCases.add(nullWhere.not(), prevExpr);
        return exprCases.getFinal();
    }

    public ExclusiveCaseUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(Case operand : cases)
            result = result.add(operand.property.mapDataChanges(
                    change.and(operand.where.mapExpr(change.mapKeys, propChanges).getWhere()), changedWhere, propChanges));
        return result;
    }
}
