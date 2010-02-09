package platform.server.logics.property.derived;

import net.jcip.annotations.Immutable;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.FunctionProperty;
import platform.server.logics.property.GroupProperty;
import platform.server.caches.Lazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
* User: ME2
* Date: 25.01.2010
* Time: 17:20:09
* To change this template use File | Settings | File Templates.
*/
@Immutable
public class NullChangeProperty<T extends PropertyInterface> extends FunctionProperty<NullChangeProperty.Interface<T>> {

    private static <T extends PropertyInterface> Collection<Interface<T>> getInterfaces(Collection<? extends GroupProperty.Interface<T>> interfaceImplements) {
        Collection<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(GroupProperty.Interface<T> implement : interfaceImplements)
            interfaces.add(new Interface<T>(interfaces.size(),implement));
        return interfaces;
    }

    public NullChangeProperty(GroupProperty<T> groupProperty) {
        super("NULL - "+groupProperty.sID, "NULL - "+groupProperty.caption, getInterfaces(groupProperty.interfaces));
    }

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        final GroupProperty.Interface<T> groupInterface;

        private Interface(int ID, GroupProperty.Interface<T> groupInterface) {
            super(ID);
            this.groupInterface = groupInterface;
        }
    }

    @Lazy
    Map<Interface<T>,GroupProperty.Interface<T>> getMapInterfaces() {
        Map<Interface<T>,GroupProperty.Interface<T>> result = new HashMap<Interface<T>, GroupProperty.Interface<T>>();
        for(Interface<T> propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.groupInterface);
        return result;
    }

    protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }
}
