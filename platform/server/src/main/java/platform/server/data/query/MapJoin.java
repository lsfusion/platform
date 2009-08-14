package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Collection;
import java.util.Map;

public class MapJoin<V,MV> extends Join<V> {
    private Join<MV> join;
    protected Map<V,MV> mapProps;

    public MapJoin(Join<MV> iJoin, Map<V, MV> iMapProps) {
        join = iJoin;
        mapProps = iMapProps;
    }

    public SourceExpr getExpr(V property) {
        return join.getExpr(mapProps.get(property));
    }

    public Collection<V> getProperties() {
        return mapProps.keySet();
    }

    public Where getWhere() {
        return join.getWhere();
    }

}
