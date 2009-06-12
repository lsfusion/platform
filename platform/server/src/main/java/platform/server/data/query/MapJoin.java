package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;
import platform.base.BaseUtils;

import java.util.Map;

public class MapJoin<V,MV> implements Join<V> {
    private Join<MV> join;
    protected Map<V,MV> mapProps;

    public MapJoin(Join<MV> iJoin, Map<V, MV> iMapProps) {
        join = iJoin;
        mapProps = iMapProps;
    }

    public SourceExpr getExpr(V property) {
        return join.getExpr(mapProps.get(property));
    }

    public Map<V, SourceExpr> getExprs() {
        return BaseUtils.join(mapProps,join.getExprs());
    }

    public Where getWhere() {
        return join.getWhere();
    }

    public Context getContext() {
        return join.getContext();
    }
}
