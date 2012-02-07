package platform.server.data.expr.where.cases;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseList;
import platform.server.data.expr.where.cases.CaseWhereInterface;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.query.Join;

import java.util.Collection;

public class JoinCaseList<U> extends CaseList<Join<U>, Join<U>,JoinCase<U>> {

    public Expr getExpr(U property) {
        ExprCaseList result = new ExprCaseList();
        for(JoinCase<U> joinCase : this)
            result.add(joinCase.where,joinCase.data.getExpr(property));
        return result.getFinal();
    }

    public Collection<U> properties;
    public JoinCaseList(Collection<U> properties) {
        this.properties = properties;
    }

    public JoinCaseList(Where where,Join<U> join) {
        this(join.getProperties());
        add(new JoinCase<U>(where,join));
    }

    public Where getWhere() {
        return getWhere(new CaseWhereInterface<Join<U>>() {
            public Where getWhere(Join<U> cCase) {
                return cCase.getWhere();
           }
        });
    }

    @Override
    public void add(Where where, Join<U> data) {
        add(new JoinCase<U>(where, data));
    }

    @Override
    public Join<U> getFinal() {
        return new CaseJoin<U>(this);
    }
}
