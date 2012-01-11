package platform.server.data.expr.where.ifs;

import platform.server.data.query.AbstractJoin;
import platform.server.data.where.Where;
import platform.server.data.query.Join;
import platform.server.data.expr.Expr;

import java.util.Collection;

public class IfJoin<U> extends AbstractJoin<U> {

    private Where ifWhere;
    private Join<U> trueJoin;
    private Join<U> falseJoin;

    public IfJoin(Where ifWhere, Join<U> join) {
        this(ifWhere, join, new NullJoin<U>(join.getProperties()));
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

    public Collection<U> getProperties() {
        return trueJoin.getProperties(); // assert что совпадает с falseJoin
    }
}
