package lsfusion.server.data.query.translate;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

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
        return new RemapJoin<>(join.translateRemoveValues(translate), mapProps);
    }
}
