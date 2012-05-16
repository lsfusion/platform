package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

public class CalcPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends CalcPropertyImplement<P, T> implements CalcPropertyInterfaceImplement<T> {

    public CalcPropertyMapImplement(CalcProperty<P> property) {
        super(property);
    }

    public CalcPropertyMapImplement(CalcProperty<P> property, Map<P, T> mapping) {
        super(property, mapping);
    }

    public DataChanges mapDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getDataChanges(change.map(mapping), propChanges, changedWhere);
    }

    public CalcPropertyMapImplement<P, T> mapChanged(IncrementType type) {
        return new CalcPropertyMapImplement<P, T>(property.getChanged(type), mapping);
    }

    public CalcPropertyValueImplement<P> mapValues(Map<T, DataObject> mapValues) {
        return new CalcPropertyValueImplement<P>(property, BaseUtils.join(mapping, mapValues));
    }

    public List<ClientAction> change(Map<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        return change(keys, env, env.getSession().getObjectValue(value, property.getType()));
    }

    public <K extends PropertyInterface> CalcPropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new CalcPropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public List<ClientAction> change(Map<T, DataObject> keys, ExecutionEnvironment env, ObjectValue objectValue) throws SQLException {
        return env.change(property, mapValues(keys).getPropertyChange(objectValue.getExpr()));
    }

    public ClassWhere<T> mapClassWhere() {
        return new ClassWhere<T>(property.getClassWhere(),mapping);
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

    public void mapNotNull(Map<T, DataObject> values, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        property.setNotNull(BaseUtils.join(mapping, values), env, notNull, check);
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
        return read(context.getSession(), interfaceValues, context.getModifier());
    }

    private Object read(DataSession session, Map<T, DataObject> interfaceValues, Modifier modifier) throws SQLException {
        return property.read(session.sql, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public ObjectValue readClasses(ExecutionContext context, Map<T, DataObject> interfaceValues) throws SQLException {
        return readClasses(context.getSession(), interfaceValues, context.getModifier());
    }

    private ObjectValue readClasses(DataSession session, Map<T, DataObject> interfaceValues, Modifier modifier) throws SQLException {
        return property.readClasses(session, BaseUtils.join(mapping, interfaceValues), modifier, session.env);
    }

    public DataChanges mapJoinDataChanges(Map<T, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getJoinDataChanges(BaseUtils.join(mapping, mapKeys), expr, where, propChanges, changedWhere);
    }

    public void fill(Set<T> interfaces, Set<CalcPropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public Map<T,ValueClass> mapCommonInterfaces() { // тут не обязательно isFull
        return crossJoin(mapping, property.getMapClasses());
    }

    @Override
    public ActionPropertyMapImplement<T> mapEditAction(String editActionSID, CalcProperty filterProperty) {
        return property.getEditAction(editActionSID, filterProperty).map(mapping);
    }
}
