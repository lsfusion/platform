package platform.server.data.expr.where.ifs;

import platform.server.data.query.AbstractJoin;
import platform.server.data.query.Join;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.Collection;

public class NullJoin<U> extends AbstractJoin<U> {

    private Collection<U> properties;
    public NullJoin(Collection<U> properties) {
        this.properties = properties;
    }

    public Expr getExpr(U property) {
        return Expr.NULL;
    }

    public Where getWhere() {
        return Where.FALSE;
    }

    public Collection<U> getProperties() {
        return properties;
    }
}
