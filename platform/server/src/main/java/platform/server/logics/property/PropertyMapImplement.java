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
import platform.server.form.instance.remote.RemoteForm;
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

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier);
    }
    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), propChanges);
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

    public Object read(DataSession session, Map<T, DataObject> interfaceValues, Modifier modifier) throws SQLException {
        return property.read(session.sql, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public MapDataChanges<T> mapDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getDataChanges(change.map(mapping), propChanges, changedWhere).map(mapping);
    }

    public MapDataChanges<T> mapJoinDataChanges(Map<T, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, joinImplement), expr, where, propChanges, changedWhere).map(mapping);
    }

    public void mapNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        property.setJoinNotNull(BaseUtils.join(mapping, mapKeys), where, session);
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

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier modifier) throws SQLException {
        return execute(keys, session, value, modifier, null, null);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return session.execute(property, mapValues(keys).getPropertyChange(session.getObjectValue(value, property.getType()).getExpr()), modifier, executeForm, mapObjects);
    }

    public void fill(Set<T> interfaces, Set<PropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public <K extends PropertyInterface> PropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new PropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public <K> PropertyImplement<P, K> mapImplement(Map<T, K> remap) {
        return new PropertyImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public Expr mapIncrementExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges newChanges, PropertyChanges prevChanges, WhereBuilder changedWhere, IncrementType incrementType) {
        return property.getIncrementExpr(BaseUtils.join(mapping, joinImplement), newChanges, prevChanges, changedWhere, incrementType);
    }
}
