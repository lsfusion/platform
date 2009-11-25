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

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P>, Comparable<P> {

    public int ID = 0;
    public PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return joinImplement.get((P) this);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier) {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(Collection<Property> depends) {
    }

    public DataChange mapGetChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return null;
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    public PropertyChange mapGetJoinChangeProperty(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return null;
    }
}
