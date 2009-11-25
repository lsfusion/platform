package platform.server.session;

import platform.base.BaseUtils;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.where.WhereBuilder;

import java.util.Map;

public abstract class TableModifier<U extends TableChanges<U>> implements Modifier<U> {

    public abstract SessionChanges getSession();

    public void modifyAdd(U changes, ValueClass valueClass) {
        SessionChanges session = getSession();
        if(session!=null && valueClass instanceof CustomClass)
            BaseUtils.putNotNull((CustomClass)valueClass,session.add,changes.add);
    }
    public void modifyRemove(U changes, ValueClass valueClass) {
        SessionChanges session = getSession();
        if(session!=null && valueClass instanceof CustomClass)
            BaseUtils.putNotNull((CustomClass)valueClass,session.remove,changes.remove);
    }
    public void modifyData(U changes, DataProperty property) {
        SessionChanges session = getSession();
        if(session!=null)
            BaseUtils.putNotNull(property,session.data,changes.data);
    }

    public abstract <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere);
}
