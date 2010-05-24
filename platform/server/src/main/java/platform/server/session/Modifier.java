package platform.server.session;

import platform.server.data.expr.Expr;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.where.WhereBuilder;

import java.util.Map;
import java.util.Collection;

public abstract class Modifier<U extends Changes<U>> {

    public abstract U used(Property property,U usedChanges);
    public abstract U newChanges();

    public abstract U fullChanges();
    public abstract SessionChanges getSession();

    public abstract <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere);

    public U getUsedChanges(Collection<Property> col) {
        U result = newChanges();
        for(Property<?> property : col)
            result = result.add(property.getUsedChanges(this));
        return result;
    }

}
