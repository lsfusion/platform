package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.sql.SQLException;

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> iProperty) {
        super(iProperty);
    }
    public PropertyMapImplement(Property<T> iProperty, Map<T, P> iMapping) {
        super(iProperty, iMapping);
    }

    // NotNull только если сессии нету
    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier, changedWhere);
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public DataChange mapGetChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return property.getChangeProperty(session, BaseUtils.join(mapping, interfaceValues), modifier, securityPolicy, externalID);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return session.getObjectValue(property.read(session,BaseUtils.join(mapping,interfaceValues),modifier),property.getType());
    }

    public PropertyChange mapGetJoinChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return property.getJoinChangeProperty(session, BaseUtils.join(mapping, interfaceValues), modifier, securityPolicy, externalID);
    }
}
