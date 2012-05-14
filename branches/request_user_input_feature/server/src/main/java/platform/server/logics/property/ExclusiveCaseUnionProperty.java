package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

public abstract class ExclusiveCaseUnionProperty extends AbstractCaseUnionProperty {

    protected abstract Iterable<Case> getCases();

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : getCases())
            exprCases.add(propCase.where.mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere(), propCase.property.mapExpr(joinImplement, propClasses, propChanges, changedWhere));
        return exprCases.getFinal();
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Where nullWhere = Where.FALSE;
        CaseExprInterface exprCases = Expr.newCases();
        for(Case propCase : getCases()) {
            WhereBuilder changedWhereCase = new WhereBuilder();
            Where caseWhere = propCase.where.mapExpr(joinImplement, propChanges, changedWhereCase).getWhere();

            WhereBuilder changedExprCase = new WhereBuilder();
            Expr caseExpr = propCase.property.mapExpr(joinImplement, propChanges, changedExprCase);

            Where changedCaseWhere = caseWhere.and(changedWhereCase.toWhere().or(changedExprCase.toWhere()));
            if(changedWhere!=null) changedWhere.add(changedCaseWhere);
            exprCases.add(changedCaseWhere, caseExpr);
            nullWhere = nullWhere.or(changedWhereCase.toWhere().and(propCase.where.mapExpr(joinImplement).getWhere()));
        }
        if(changedWhere!=null) changedWhere.add(nullWhere);
        exprCases.add(nullWhere.not(), prevExpr);
        return exprCases.getFinal();
    }

    public ExclusiveCaseUnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = new DataChanges();
        for(Case operand : getCases())
            result = result.add(operand.property.mapDataChanges(
                    change.and(operand.where.mapExpr(change.getMapExprs(), propChanges).getWhere()), changedWhere, propChanges));
        return result;
    }
}
