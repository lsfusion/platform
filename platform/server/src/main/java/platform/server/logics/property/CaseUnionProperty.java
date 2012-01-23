package platform.server.logics.property;

import platform.base.Result;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class CaseUnionProperty extends AbstractCaseUnionProperty {

    private final List<Case> cases;
    protected Iterable<Case> getCases() {
        return cases;
    }

    public CaseUnionProperty(String sID, String caption, List<Interface> interfaces, List<Case> cases) {
        super(sID, caption, interfaces);
        this.cases = cases;

        finalizeInit();
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases)
            exprCases.add(propCase.where.mapExpr(joinImplement, propChanges, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, propChanges, changedWhere));
        return exprCases.getFinal();
    }

    // вообще Case W1 E1, W2 E2, Wn En - эквивалентен Exclusive (W1 - E1, W2 AND !W1 - E2, ... )
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // вообще инкрементальность делается следующим образом
        // Wi AND (OR(Cwi) OR CЕi) AND !OR(Wi-1) - Ei или вставлять прмежуточные (но у 1-го подхода - не надо отрезать сзади ничего, changed более релевантен)
        Where changedUpWheres = Where.FALSE;
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases) {
            WhereBuilder changedWhereCase = new WhereBuilder();
            Where caseWhere = propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase).getWhere();
            changedUpWheres = changedUpWheres.or(changedWhereCase.toWhere());

            WhereBuilder changedExprCase = new WhereBuilder();
            Expr caseExpr = propCase.property.mapExpr(joinImplement, propChanges, changedExprCase);

            if(changedWhere!=null) changedWhere.add(changedWhereCase.toWhere().or(changedExprCase.toWhere()));
            exprCases.add(caseWhere.and(changedUpWheres.or(changedExprCase.toWhere())), caseExpr);
            exprCases.add(caseWhere, prevExpr);
        }
        return exprCases.getFinal();
    }

    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(Case operand : cases) {
            Where caseWhere = operand.where.mapExpr(change.mapKeys, propChanges).getWhere();
            result = result.add(operand.property.mapDataChanges(change.and(caseWhere), changedWhere, propChanges));
            change = change.and(caseWhere.not());
        }
        return result;
    }
}
