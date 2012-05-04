package platform.server.data.query;

import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

public class RemapJoin<V,MV> extends AbstractJoin<V> {
    private Join<MV> join;
    protected Map<V,MV> mapProps;

    public RemapJoin(Join<MV> join, Map<V, MV> mapProps) {
        this.join = join;
        this.mapProps = mapProps;
    }

    public Expr getExpr(V property) {
        return join.getExpr(mapProps.get(property));
    }

    public Collection<V> getProperties() {
        return mapProps.keySet();
    }

    public Where getWhere() {
        return join.getWhere();
    }

    public Join<V> translateRemoveValues(MapValuesTranslate translate) {
        return new RemapJoin<V, MV>(join.translateRemoveValues(translate), mapProps);
    }
}
