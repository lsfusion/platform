package platform.server.logics.property.derived;

import platform.server.logics.property.FunctionProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.base.BaseUtils;

import java.util.*;

public class ExprProperty<T extends PropertyInterface> extends FunctionProperty<ExprProperty.Interface<T>> {

    private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(PropertyChange<T> change) {
        List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(T propertyInterface : change.mapKeys.keySet())
            interfaces.add(new Interface<T>(interfaces.size(),propertyInterface));
        return interfaces;
    }

    final PropertyChange<T> change;
    
    public ExprProperty(PropertyChange<T> change) {
        super("pc"+change.hashCode(), change.toString(), getInterfaces(change));
        this.change = change;
    }

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        final T mapInterface;

        public Interface(int ID, T mapInterface) {
            super(ID);
            this.mapInterface = mapInterface;
        }
    }

    public Map<T,Interface<T>> getMapInterfaces() {
        Map<T,Interface<T>> result = new HashMap<T, Interface<T>>();
        for(Interface<T> propertyInterface : interfaces)
            result.put(propertyInterface.mapInterface, propertyInterface);
        return result;
    }
    
    protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return change.getQuery("value").join(BaseUtils.join(getMapInterfaces(),joinImplement)).getExpr("value");
    }
}
