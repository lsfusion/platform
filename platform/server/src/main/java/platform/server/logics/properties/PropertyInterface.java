package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.Map;

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P>, Comparable<P> {

    public int ID = 0;
    public PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return joinImplement.get((P) this);
    }

    public void mapFillDepends(Collection<Property> depends) {
    }

    public MapChangeDataProperty<P> mapGetChangeProperty(Map<P, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return null;
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

}
