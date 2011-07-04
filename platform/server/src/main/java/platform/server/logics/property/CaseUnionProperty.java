package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.util.*;

public class CaseUnionProperty extends AbstractCaseUnionProperty {

    private List<Case> cases = new ArrayList<Case>();
    public void addCase(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
        addCase(where, property, false);
    }
    public void addCase(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property, boolean begin) {
        Case propCase = new Case(where, property);
        if(begin)
            cases.add(0, propCase);
        else
            cases.add(propCase);
    }
    protected Collection<Case> getCases() {
        return cases;
    }

    public CaseUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases)
            exprCases.add(propCase.where.mapExpr(joinImplement, modifier, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, modifier, changedWhere));
        return exprCases.getFinal();
    }

    // вообще Case W1 E1, W2 E2, Wn En - эквивалентен Exclusive (W1 - E1, W2 AND !W1 - E2, ... )
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, Expr prevExpr, WhereBuilder changedWhere) {
        // вообще инкрементальность делается следующим образом
        // Wi AND (OR(Cwi) OR CЕi) AND !OR(Wi-1) - Ei или вставлять прмежуточные (но у 1-го подхода - не надо отрезать сзади ничего, changed более релевантен)
        Where changedUpWheres = Where.FALSE;
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : cases) {
            WhereBuilder changedWhereCase = new WhereBuilder();
            Where caseWhere = propCase.where.mapExpr(joinImplement, modifier, changedWhereCase).getWhere();
            changedUpWheres = changedUpWheres.or(changedWhereCase.toWhere());

            WhereBuilder changedExprCase = new WhereBuilder();
            Expr caseExpr = propCase.property.mapExpr(joinImplement, modifier, changedExprCase);

            Where changedCase = changedUpWheres.or(changedExprCase.toWhere());
            if(changedWhere!=null) changedWhere.add(changedWhereCase.toWhere().or(changedExprCase.toWhere()));
            exprCases.add(caseWhere.and(changedCase), caseExpr);
            exprCases.add(caseWhere, prevExpr);
        }
        return exprCases.getFinal();
    }

    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(Case operand : cases) {
            Where caseWhere = operand.where.mapExpr(change.mapKeys, modifier).getWhere();
            result = result.add(operand.property.mapDataChanges(change.and(caseWhere), changedWhere, modifier));
            change = change.and(caseWhere.not());
        }
        return result;
    }
}
