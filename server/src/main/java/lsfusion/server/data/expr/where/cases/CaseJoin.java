package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class CaseJoin<U> extends AbstractJoin<U> {

    private final JoinCaseList<U> cases;

    public CaseJoin(JoinCaseList<U> cases) {
        this.cases = cases;
    }

    public CaseJoin(Where where, Join<U> join) {
        this(new JoinCaseList<>(where, join));
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
        return new CaseJoin<>(cases.translateRemoveValues(translate));
    }
}
