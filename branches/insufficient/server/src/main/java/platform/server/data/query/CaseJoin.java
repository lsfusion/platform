package platform.server.data.query;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.Collection;

public class CaseJoin<U> extends Join<U> {

    private final JoinCaseList<U> cases;

    private final Collection<U> properties;

    public CaseJoin(JoinCaseList<U> cases, Collection<U> properties) {
        this.properties = properties;
        this.cases = cases;
    }

    public CaseJoin(Where where, Join<U> join) {
        this(new JoinCaseList<U>(where, join), join.getProperties());
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
        return properties;
    }
}
