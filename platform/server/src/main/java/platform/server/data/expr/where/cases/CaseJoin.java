package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

public class CaseJoin<U> extends AbstractJoin<U> {

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

    public ImSet<U> getProperties() {
        return cases.properties;
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new CaseJoin<U>(cases.translateRemoveValues(translate));
    }
}
