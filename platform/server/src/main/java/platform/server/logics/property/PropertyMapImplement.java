package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends PropertyImplement<P, T> implements PropertyInterfaceImplement<T> {

    public PropertyMapImplement(Property<P> property) {
        super(property);
    }

    public PropertyMapImplement(Property<P> property, Map<P, T> mapping) {
        super(property, mapping);
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier, changedWhere);
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier);
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement));
    }

    public void mapFillDepends(Collection<Property> depends) {
        depends.add(property);
    }

    public Object read(ExecutionContext context, Map<T, DataObject> interfaceValues) throws SQLException {
        return read(context.getSession(), interfaceValues, context.getModifier());
    }

    public Object read(DataSession session, Map<T, DataObject> interfaceValues, Modifier<? extends Changes> modifier) throws SQLException {
        return property.read(session.sql, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public MapDataChanges<T> mapDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return property.getDataChanges(change.map(mapping), changedWhere, modifier).map(mapping);
    }

    public MapDataChanges<T> mapJoinDataChanges(Map<T, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, joinImplement), expr, where, modifier, changedWhere).map(mapping);
    }

    public void mapNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        property.setJoinNotNull(BaseUtils.join(mapping, mapKeys), where, session, BL);
    }

    public PropertyMapImplement<?, T> mapChangeImplement() {
        return property.getChangeImplement(new Result<Property>()).map(mapping);
    }

    public PropertyObjectInstance<P> mapObjects(Map<T, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new PropertyObjectInstance<P>(property, BaseUtils.join(mapping, mapObjects));
    }

    public PropertyValueImplement<P> mapValues(Map<T, DataObject> mapValues) {
        return new PropertyValueImplement<P>(property, BaseUtils.join(mapping, mapValues));
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier) throws SQLException {
        return session.execute(property, mapValues(keys).getPropertyChange(session.getObjectValue(value, property.getType()).getExpr()), modifier, null, null);
    }

    public void fill(Set<T> interfaces, Set<PropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public <K extends PropertyInterface> PropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new PropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }
}
