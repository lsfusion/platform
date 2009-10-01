package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.caches.TwinLazy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Collection;

@Immutable
public class CaseJoin<U> extends Join<U> {

    private final JoinCaseList<U> cases;

    private final Collection<U> properties;

    public CaseJoin(JoinCaseList<U> iCases, Collection<U> iProperties) {
        properties = iProperties;
        cases = iCases;
    }

    public CaseJoin(Where where,Join<U> join) {
        this(new JoinCaseList<U>(where,join), join.getExprs().keySet());
    }

    @Lazy
    public Where getWhere() {
        return cases.getWhere();
    }

    @Lazy
    public SourceExpr getExpr(U property) {
        return cases.getExpr(property);
    }

    public Collection<U> getProperties() {
        return properties;
    }
}
