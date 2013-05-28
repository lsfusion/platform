package platform.server.data.expr.where.ifs;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.Expr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

public class NullJoin<U> extends AbstractJoin<U> {

    private ImSet<U> properties;
    public NullJoin(ImSet<U> properties) {
        this.properties = properties;
    }

    public Expr getExpr(U property) {
        return Expr.NULL;
    }

    public Where getWhere() {
        return Where.FALSE;
    }

    public ImSet<U> getProperties() {
        return properties;
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return this;
    }
}
