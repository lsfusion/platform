package platform.server.data.expr.where.cases;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.query.Join;

import java.util.Collection;

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

    public Collection<U> getProperties() {
        return cases.properties;
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        MapTranslate translator = translate.mapKeys();

        JoinCaseList<U> transCaseList = new JoinCaseList<U>(cases.properties);
        for(JoinCase<U> joinCase : cases)
            transCaseList.add(new JoinCase<U>(joinCase.where.translateOuter(translator), joinCase.data.translateRemoveValues(translate)));
        return new CaseJoin<U>(transCaseList);
    }
}
