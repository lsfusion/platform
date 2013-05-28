package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

public class RemapJoin<V,MV> extends AbstractJoin<V> {
    private Join<MV> join;
    protected ImRevMap<V,MV> mapProps;

    public RemapJoin(Join<MV> join, ImRevMap<V, MV> mapProps) {
        this.join = join;
        this.mapProps = mapProps;
    }

    public Expr getExpr(V property) {
        return join.getExpr(mapProps.get(property));
    }

    public ImSet<V> getProperties() {
        return mapProps.keys();
    }

    public Where getWhere() {
        return join.getWhere();
    }

    public Join<V> translateRemoveValues(MapValuesTranslate translate) {
        return new RemapJoin<V, MV>(join.translateRemoveValues(translate), mapProps);
    }
}
