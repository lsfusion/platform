package platform.server.data.expr.where.cases;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.query.Join;

import java.util.Collection;

public class CaseJoin<U> extends Join<U> {

    private final JoinCaseList<U> cases;

    public CaseJoin(JoinCaseList<U> cases) {
        this.cases = cases;
    }

    public CaseJoin(Where where, Join<U> join) {
        this(new JoinCaseList<U>(where, join));
    }

    @IdentityLazy
    public Where getWhere() {
        return cases.getWhere();
    }

    @IdentityLazy
    public Expr getExpr(U property) {
        return cases.getExpr(property);
    }

    public Collection<U> getProperties() {
        return cases.properties;
    }
}
