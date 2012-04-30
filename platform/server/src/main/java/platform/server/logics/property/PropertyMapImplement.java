package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.reverse;

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

    public Set<OldProperty> mapOldDepends() {
        return property.getOldDepends();
    }

    public Object read(ExecutionContext context, Map<T, DataObject> interfaceValues) throws SQLException {
        return read(context.getSession(), interfaceValues, context.getModifier());
    }

    public Object read(DataSession session, Map<T, DataObject> interfaceValues, Modifier modifier) throws SQLException {
        return property.read(session.sql, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> interfaceValues, Modifier modifier) throws SQLException {
        return property.readClasses(session, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public MapDataChanges<T> mapDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getDataChanges(change.map(mapping), propChanges, changedWhere).map(mapping);
    }

    public MapDataChanges<T> mapJoinDataChanges(Map<T, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, mapKeys), expr, where, propChanges, changedWhere).map(mapping);
    }

    public void mapNotNull(Map<T, DataObject> values, DataSession session, Modifier modifier, boolean notNull, boolean check) throws SQLException {
        property.setNotNull(BaseUtils.join(mapping, values), session, modifier, notNull, check);
    }

    public PropertyMapImplement<?, T> mapChangeImplement(Map<T, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        PropertyMapImplement<?, P> changeImplement = property.modifyChangeImplement(new Result<Property>(), BaseUtils.join(mapping, interfaceValues), session, modifier);
        if(changeImplement!=null)
            return changeImplement.map(mapping);
        return null;
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
        return execute(keys, session, session.getObjectValue(value, property.getType()), modifier, executeForm, mapObjects);
    }

    private List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, ObjectValue objectValue, Modifier modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return session.execute(property, mapValues(keys).getPropertyChange(objectValue.getExpr()), modifier, executeForm, mapObjects);
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

    public ClassWhere<T> mapClassWhere() {
        return new ClassWhere<T>(property.getClassWhere(),mapping);
    }

    public Map<T,ValueClass> mapCommonInterfaces() {
        return crossJoin(mapping, property.getCommonClasses().interfaces);
    }

    public <L extends PropertyInterface> void mapDerivedChange(PropertyMapImplement<L, T> where, int options) {
        property.setDerivedChange(where.map(BaseUtils.reverse(mapping)), options);
    }
    
    public boolean mapIsFull(Collection<T> interfaces) {
        if(interfaces.isEmpty()) // оптимизация
            return true;

        Collection<P> checkInterfaces = new ArrayList<P>();
        for(Map.Entry<P, T> entry : mapping.entrySet())
            if(interfaces.contains(entry.getValue()))
                checkInterfaces.add(entry.getKey());

        // если все собрали интерфейсы
        return checkInterfaces.size() >= interfaces.size() && property.isFull(checkInterfaces);
    }
    
    public PropertyMapImplement<P, T> mapChanged(IncrementType type) {
        return new PropertyMapImplement<P, T>(property.getChanged(type), mapping);
    }
}
