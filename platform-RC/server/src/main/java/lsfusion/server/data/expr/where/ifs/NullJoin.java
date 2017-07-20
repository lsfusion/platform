package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.AbstractJoin;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;

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
