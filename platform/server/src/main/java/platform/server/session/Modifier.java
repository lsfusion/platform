package platform.server.session;

import platform.base.BaseUtils;
import platform.base.MutableObject;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.DerivedChange;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class Modifier<U extends Changes<U>> extends MutableObject {

    public abstract U preUsed(Property property);
    public U postUsed(Property property, U changes) {
        return changes;
    }
    public abstract U newChanges();

    protected abstract U newFullChanges();

    U fullChanges; // чтобы кэшировать ссылку, можно было бы TwinLazy использовать но будет memory leak
    @ManualLazy
    public U fullChanges() {
        U newFullChanges = newFullChanges();
        if (fullChanges == null || !BaseUtils.hashEquals(fullChanges, newFullChanges))
            fullChanges = newFullChanges;
        return fullChanges;
    }

    public abstract ExprChanges getSession();
    public SimpleChanges getChanges() {
        return getSession().getUsedChanges();        
    }

    public abstract <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere);

    public U getUsedChanges(Collection<Property> col) {
        U result = newChanges();
        for(Property<?> property : col)
            result = result.add(property.getUsedChanges(this));
        return result;
    }

    public U getUsedDataChanges(Collection<Property> col) {
        U result = newChanges();
        for(Property<?> property : col)
            result = result.add(property.getUsedDataChanges(this));
        return result;
    }

    public abstract boolean neededClass(Changes changes);

    public Modifier<? extends Changes> getApplyStart() {
        return Property.defaultModifier;
    }

    public U getApplyUsedChanges(Set<Property> props) {
        return getUsedChanges(props);
    }

}

