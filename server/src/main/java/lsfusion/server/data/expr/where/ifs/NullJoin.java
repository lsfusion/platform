package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class NullJoin<U> extends AbstractJoin<U> {

    private NullJoin() {
    }
    private static final NullJoin instance = new NullJoin();
    public static <T> NullJoin<T> getInstance() {
        return instance;
    }

    public Expr getExpr(U property) {
        return Expr.NULL();
    }

    public Where getWhere() {
        return Where.FALSE();
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return this;
    }
}
