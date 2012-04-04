package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OldProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {
    public final Property<T> property;

    public OldProperty(Property<T> property) {
        super("OLD_" + property.getSID(), property.caption + " (в БД)", (List<T>)property.interfaces);
        this.property = property;
    }

    @Override
    public Set<OldProperty> getOldDepends() {
        return Collections.<OldProperty>singleton(this);
    }

    protected Expr calculateExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return property.getExpr(joinImplement); // возвращаем старое значение
    }
}
