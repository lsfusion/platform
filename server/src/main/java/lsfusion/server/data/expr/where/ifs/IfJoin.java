package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class IfJoin<U> extends AbstractJoin<U> {

    private Where ifWhere;
    private Join<U> trueJoin;
    private Join<U> falseJoin;

    public IfJoin(Where ifWhere, Join<U> join) {
        this(ifWhere, join, NullJoin.getInstance());
    }

    public IfJoin(Where ifWhere, Join<U> trueJoin, Join<U> falseJoin) {
        this.ifWhere = ifWhere;
        this.trueJoin = trueJoin;
        this.falseJoin = falseJoin;
    }

    public Expr getExpr(U property) {
        return trueJoin.getExpr(property).ifElse(ifWhere, falseJoin.getExpr(property));
    }

    public Where getWhere() {
        return ifWhere.ifElse(trueJoin.getWhere(),falseJoin.getWhere());
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new IfJoin<>(ifWhere.translateOuter(translate.mapKeys()), trueJoin.translateRemoveValues(translate), falseJoin.translateRemoveValues(translate));
    }
}
