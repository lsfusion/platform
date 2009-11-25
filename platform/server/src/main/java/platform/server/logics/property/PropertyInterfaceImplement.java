package platform.server.logics.property;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.sql.SQLException;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    Expr mapExpr(Map<P, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere);

    abstract void mapFillDepends(Collection<Property> depends);

    DataChange mapGetChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException;

    ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier) throws SQLException;

    PropertyChange mapGetJoinChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException;
}
