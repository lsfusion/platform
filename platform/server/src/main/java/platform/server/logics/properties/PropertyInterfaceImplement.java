package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.*;
import platform.server.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.sql.SQLException;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere);

    abstract void mapFillDepends(Collection<Property> depends);

    ChangeProperty mapGetChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException;

    ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier) throws SQLException;

    DataChangeProperty mapGetJoinChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException;
}
