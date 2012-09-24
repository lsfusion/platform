package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.CalcPropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.reverse;
import static platform.base.BaseUtils.rightCrossJoin;

public class CalcPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends CalcPropertyImplement<P, T> implements CalcPropertyInterfaceImplement<T> {

    public CalcPropertyMapImplement(CalcProperty<P> property) {
        super(property);
    }

    public CalcPropertyMapImplement(CalcProperty<P> property, Map<P, T> mapping) {
        super(property, mapping);
        
        assert !mapping.containsValue(null);
    }

    public DataChanges mapDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getDataChanges(change.map(mapping), propChanges, changedWhere);
    }

    public CalcPropertyMapImplement<P, T> mapOld() {
        return new CalcPropertyMapImplement<P, T>(property.getOld(), mapping);
    }

    public CalcPropertyMapImplement<P, T> mapChanged(IncrementType type) {
        return new CalcPropertyMapImplement<P, T>(property.getChanged(type), mapping);
    }

    public CalcPropertyValueImplement<P> mapValues(Map<T, DataObject> mapValues) {
        return new CalcPropertyValueImplement<P>(property, BaseUtils.join(mapping, mapValues));
    }

    public void change(Map<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        change(keys, env, env.getSession().getObjectValue(value, property.getType()));
    }

    public <K extends PropertyInterface> CalcPropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new CalcPropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public void change(Map<T, DataObject> keys, ExecutionEnvironment env, ObjectValue objectValue) throws SQLException {
        env.change(property, mapValues(keys).getPropertyChange(objectValue.getExpr()));
    }

    public void change(ExecutionEnvironment env, PropertyChange<T> change) throws SQLException {
        env.change(property, change.map(mapping));
    }

    public Map<T,ValueClass> mapInterfaceClasses() {
        return mapInterfaceClasses(false);
    }
    public Map<T,ValueClass> mapInterfaceClasses(boolean full) {
        return rightCrossJoin(mapping, property.getInterfaceClasses(full));
    }
    public ClassWhere<T> mapClassWhere() {
        return mapClassWhere(false);
    }
    public ClassWhere<T> mapClassWhere(boolean full) {
        return new ClassWhere<T>(property.getClassWhere(full),mapping);
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

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), modifier);
    }
    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement), propChanges);
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement) {
        return property.getExpr(BaseUtils.join(mapping, joinImplement));
    }

    public void mapFillDepends(Set<CalcProperty> depends) {
        depends.add(property);
    }

    public Set<OldProperty> mapOldDepends() {
        return property.getOldDepends();
    }

    public Object read(ExecutionContext context, Map<T, DataObject> interfaceValues) throws SQLException {
        return property.read(context.getSession().sql, BaseUtils.join(mapping, interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    public ObjectValue readClasses(ExecutionContext context, Map<T, DataObject> interfaceValues) throws SQLException {
        return property.readClasses(context.getSession(), BaseUtils.join(mapping, interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    public Collection<DataProperty> mapChangeProps() {
        return property.getChangeProps();
    }

    public DataChanges mapJoinDataChanges(Map<T, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, mapKeys), expr, where, propChanges, changedWhere);
    }

    public void fill(Set<T> interfaces, Set<CalcPropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public Collection<T> getInterfaces() {
        return mapping.values();
    }

    @Override
    public ActionPropertyMapImplement<?, T> mapEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, P> editAction = property.getEditAction(editActionSID, filterProperty);
        return editAction == null ? null : editAction.map(mapping);
    }
    
    public Map<T, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue) {
        Map<P, ValueClass> commonClasses = property.getInterfaceCommonClasses(commonValue);

        Map<T, ValueClass> result = new HashMap<T, ValueClass>();
        for (Map.Entry<P, T> entry : mapping.entrySet()) {
            ValueClass commonClass = CalcProperty.or(commonClasses.get(entry.getKey()), result.get(entry.getValue()));
            if(commonClass!=null)
                result.put(entry.getValue(), commonClass);
        }
        return result;
    }

    public CalcPropertyObjectInstance<P> mapObjects(Map<T, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new CalcPropertyObjectInstance<P>(property, BaseUtils.join(mapping, mapObjects));
    }
    
    public ClassWhere<Object> mapClassValueWhere() {
        return property.getClassValueWhere().remap(BaseUtils.<Object, P, String, Object>merge(mapping, Collections.singletonMap("value", "value")));
    }

    public <I extends PropertyInterface> boolean mapIntersect(CalcPropertyMapImplement<I, T> implement) {
        return property.intersectFull(implement.property, BaseUtils.rightCrossValues(implement.mapping, mapping));
    }

    public ActionPropertyMapImplement<?, T> getSetNotNullAction(boolean notNull) {
        ActionPropertyMapImplement<?, P> action = property.getSetNotNullAction(notNull);
        if(action!=null)
            return action.map(mapping);
        return null;
    }
}
