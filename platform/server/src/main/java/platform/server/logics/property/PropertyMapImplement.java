package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

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

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier, changedWhere);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement));
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public Object read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) throws SQLException {
        return property.read(session.sql,BaseUtils.join(mapping,interfaceValues),modifier,session.env);
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
    public PropertyObjectInstance<T> mapObjects(Map<P, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new PropertyObjectInstance<T>(property, BaseUtils.join(mapping, mapObjects));        
    }
    public PropertyValueImplement<T> mapValues(Map<P, DataObject> mapValues) {
        return new PropertyValueImplement<T>(property, BaseUtils.join(mapping, mapValues));        
    }

    public List<ClientAction> execute(Map<P, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier) throws SQLException {
        return session.execute(property, mapValues(keys).getPropertyChange(session.getObjectValue(value, property.getType()).getExpr()), modifier, null, null);
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        properties.add(this);
    }

    public <K extends PropertyInterface> PropertyMapImplement<T,K> map(Map<P,K> remap) {
        return new PropertyMapImplement<T,K>(property,BaseUtils.join(mapping,remap));
    }
}
