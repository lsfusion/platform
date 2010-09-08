package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> property) {
        super(property);
    }
    public PropertyMapImplement(Property<T> property, Map<T, P> mapping) {
        super(property, mapping);
    }

    // NotNull только если сессии нету
    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier, changedWhere);
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) throws SQLException {
        return property.readClasses(session,BaseUtils.join(mapping,interfaceValues),modifier);
    }

    public MapDataChanges<P> mapDataChanges(PropertyChange<P> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return property.getDataChanges(change.map(mapping), changedWhere, modifier).map(mapping);
    }

    public MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, joinImplement), expr, where, modifier, changedWhere).map(mapping);
    }

    public PropertyMapImplement<?,P> mapChangeImplement() {
        return property.getChangeImplement().map(mapping);
    }
    public PropertyValueImplement<T> mapValues(Map<P, DataObject> mapValues) {
        return new PropertyValueImplement<T>(property, BaseUtils.join(mapping, mapValues));        
    }
    public List<ClientAction> execute(Map<P, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects, GroupObjectInstance groupObject, Map<ObjectInstance, DataObject> mapDataValues) throws SQLException {
        return mapValues(keys).execute(session, value, modifier, executeForm, BaseUtils.nullJoin(mapping, mapObjects), groupObject, mapDataValues);
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        properties.add(this);
    }

    public <K extends PropertyInterface> PropertyMapImplement<T,K> map(Map<P,K> remap) {
        return new PropertyMapImplement<T,K>(property,BaseUtils.join(mapping,remap));
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj || obj instanceof PropertyMapImplement && property.equals(((PropertyMapImplement)obj).property) && mapping.equals(((PropertyMapImplement)obj).mapping); 
    }

    @Override
    public int hashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
