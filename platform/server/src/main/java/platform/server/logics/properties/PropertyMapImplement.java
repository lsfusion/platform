package platform.server.logics.properties;

import platform.base.BaseUtils;
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

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, WhereBuilder changedWhere, Collection<Property> noUpdateProps) {
        return property.getSourceExpr(BaseUtils.join(mapping, joinImplement), session, defaultProps, noUpdateProps, changedWhere);
    }

    public boolean mapFillChanges(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate, Map<DataProperty, DefaultData> defaultProps) {
        return property.fillChanges(changedProperties, changes, defaultProps, noUpdate);
    }

    public MapChangeDataProperty<P> mapGetChangeProperty(Map<P, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        MapChangeDataProperty<T> operandChange = property.getChangeProperty(BaseUtils.join(mapping,interfaceClasses), securityPolicy, externalID);
        if(operandChange!=null)
            return new MapChangeDataProperty<P>(operandChange,mapping,false);
        else
            return null;
    }
}
