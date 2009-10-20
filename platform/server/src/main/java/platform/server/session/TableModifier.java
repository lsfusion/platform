package platform.server.session;

import platform.base.BaseUtils;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.where.WhereBuilder;

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

    public abstract <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere);
}
