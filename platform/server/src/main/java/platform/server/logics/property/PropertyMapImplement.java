package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.sql.SQLException;

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> property) {
        super(property);
    }
    public PropertyMapImplement(Property<T> property, Map<T, P> mapping) {
        super(property, mapping);
    }

    // NotNull только если сессии нету
    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier, changedWhere);
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return session.getObjectValue(property.read(session,BaseUtils.join(mapping,interfaceValues),modifier),property.getType());
    }

    public DataChanges mapDataChanges(PropertyChange<P> change, WhereBuilder changedWhere, TableModifier<? extends TableChanges> modifier) {
        return property.getDataChanges(change.map(mapping), changedWhere, modifier);
    }

    public DataChanges mapJoinDataChanges(Map<P, ? extends Expr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, TableModifier<? extends TableChanges> modifier) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, joinImplement), expr, where, modifier, changedWhere);
    }

    public PropertyValueImplement mapChangeProperty(Map<P,DataObject> mapValues) {
        return property.getChangeProperty(BaseUtils.join(mapping, mapValues));
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        properties.add(this);
    }

    public <K extends PropertyInterface> PropertyMapImplement<T,K> map(Map<P,K> remap) {
        return new PropertyMapImplement<T,K>(property,BaseUtils.join(mapping,remap));
    }
}
