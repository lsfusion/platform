package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.Map;

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> iProperty) {
        super(iProperty);
    }
    public PropertyMapImplement(Property<T> iProperty, Map<T, P> iMapping) {
        super(iProperty, iMapping);
    }

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, Property.TableDepends<? extends Property.TableUsedChanges> depends, WhereBuilder changedWhere) {
        return property.getSourceExpr(BaseUtils.join(mapping, joinImplement), session, usedDefault, depends, changedWhere);
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public MapChangeDataProperty<P> mapGetChangeProperty(Map<P, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        MapChangeDataProperty<T> operandChange = property.getChangeProperty(BaseUtils.join(mapping,interfaceClasses), securityPolicy, externalID);
        if(operandChange!=null)
            return new MapChangeDataProperty<P>(operandChange,mapping,false);
        else
            return null;
    }
}
