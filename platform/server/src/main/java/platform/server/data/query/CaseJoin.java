package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseList;
import platform.server.data.query.exprs.cases.CaseWhereInterface;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.where.Where;
import platform.server.caches.Lazy;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import net.jcip.annotations.Immutable;

@Immutable
public class CaseJoin<U> extends CaseList<Join<U>,JoinCase<U>> implements Join<U> {

    private final Collection<U> properties;

    public CaseJoin(Collection<U> iProperties) {
        properties = iProperties;
    }

    public CaseJoin(Where where,Join<U> join) {
        add(new JoinCase<U>(where, join));
        properties = join.getExprs().keySet();
    }

    @Lazy
    public Where getWhere() {
        return getWhere(new CaseWhereInterface<Join<U>>() {
            public Where getWhere(Join<U> cCase) {
                return cCase.getWhere();
            }
        });
    }

    @Lazy
    public SourceExpr getExpr(U property) {
        ExprCaseList result = new ExprCaseList();
        for(JoinCase<U> joinCase : this)
            result.add(joinCase.where,joinCase.data.getExpr(property));
        return result.getExpr();
    }

    @Lazy
    public Map<U, SourceExpr> getExprs() {
        Map<U,SourceExpr> exprs = new HashMap<U,SourceExpr>();
        for(U property : properties)
            exprs.put(property,getExpr(property));
        return exprs;
    }

    @Lazy
    public Context getContext() {
        Context result = new Context();
        for(JoinCase<U> joinCase : this) {
            joinCase.where.fillContext(result, false);
            result.add(joinCase.data.getContext());
        }
        return result;
    }
}
