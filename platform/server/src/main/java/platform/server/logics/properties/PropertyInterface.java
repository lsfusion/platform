package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.session.DataChanges;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P> {

    public int ID = 0;
    public PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, WhereBuilder changedWhere, Collection<Property> noUpdateProps) {
        return joinImplement.get((P) this);
    }

    public boolean mapFillChanges(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate, Map<DataProperty, DefaultData> defaultProps) {
        return false;
    }

    public MapChangeDataProperty<P> mapGetChangeProperty(Map<P, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return null;
    }
}
