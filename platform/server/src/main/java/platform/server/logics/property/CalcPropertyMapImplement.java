package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.SFunctionSet;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.CalcPropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.sql.SQLException;

public class CalcPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends CalcPropertyRevImplement<P, T> implements CalcPropertyInterfaceImplement<T> {

    public CalcPropertyMapImplement(CalcProperty<P> property) {
        super(property, MapFact.<P, T>EMPTYREV());
    }
    
    public CalcPropertyMapImplement(CalcProperty<P> property, ImRevMap<P, T> mapping) {
        super(property, mapping);
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

    public CalcPropertyValueImplement<P> mapValues(ImMap<T, DataObject> mapValues) {
        return new CalcPropertyValueImplement<P>(property, mapping.join(mapValues));
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        change(keys, env, env.getSession().getObjectValue(property.getValueClass(), value));
    }

    public <K extends PropertyInterface> CalcPropertyMapImplement<P, K> map(ImRevMap<T, K> remap) {
        return new CalcPropertyMapImplement<P, K>(property, mapping.join(remap));
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, ObjectValue objectValue) throws SQLException {
        env.change(property, mapValues(keys).getPropertyChange(objectValue.getExpr()));
    }

    public void change(ExecutionEnvironment env, PropertyChange<T> change) throws SQLException {
        env.change(property, change.map(mapping));
    }

    public ImMap<T,ValueClass> mapInterfaceClasses() {
        return mapInterfaceClasses(false);
    }
    public ImMap<T,ValueClass> mapInterfaceClasses(boolean full) {
        return mapping.rightCrossJoin(property.getInterfaceClasses(full));
    }
    public ClassWhere<T> mapClassWhere() {
        return mapClassWhere(false);
    }
    public ClassWhere<T> mapClassWhere(boolean full) {
        return new ClassWhere<T>(property.getClassWhere(full),mapping);
    }

    public boolean mapIsFull(ImSet<T> interfaces) {
        if(interfaces.isEmpty()) // оптимизация
            return true;

        ImSet<P> checkInterfaces = mapping.filterValues(interfaces).keys();

        // если все собрали интерфейсы
        return checkInterfaces.size() >= interfaces.size() && property.isFull(checkInterfaces);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) {
        return property.getExpr(mapping.join(joinImplement), modifier);
    }
    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return property.getExpr(mapping.join(joinImplement), propChanges);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement) {
        return property.getExpr(mapping.join(joinImplement));
    }

    public void mapFillDepends(MSet<CalcProperty> depends) {
        depends.add(property);
    }

    public ImSet<OldProperty> mapOldDepends() {
        return property.getOldDepends();
    }

    public Object read(ExecutionContext context, ImMap<T, DataObject> interfaceValues) throws SQLException {
        return property.read(context.getSession().sql, mapping.join(interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    public ObjectValue readClasses(ExecutionContext context, ImMap<T, DataObject> interfaceValues) throws SQLException {
        return property.readClasses(context.getSession(), mapping.join(interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    public ImSet<DataProperty> mapChangeProps() {
        return property.getChangeProps();
    }

    public boolean mapIsComplex() {
        return property.isComplex();
    }

    public DataChanges mapJoinDataChanges(ImMap<T, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return property.getJoinDataChanges(mapping.join(mapKeys), expr, where, propChanges, changedWhere);
    }

    public void fill(MSet<T> interfaces, MSet<CalcPropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public ImSet<T> getInterfaces() {
        return mapping.valuesSet();
    }

    @Override
    public ActionPropertyMapImplement<?, T> mapEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, P> editAction = property.getEditAction(editActionSID, filterProperty);
        return editAction == null ? null : editAction.map(mapping);
    }
    
    public ImMap<T, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue) {
        return mapping.crossJoin(property.getInterfaceCommonClasses(commonValue));
    }

    public ClassWhere<Object> mapClassValueWhere() {
        return property.getClassValueWhere().remap(MapFact.<Object, Object>addRevExcl(mapping, "value", "value"));
    }

    public CalcPropertyObjectInstance<P> mapObjects(ImMap<T, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new CalcPropertyObjectInstance<P>(property, mapping.join(mapObjects));
    }
    
    public <I extends PropertyInterface> boolean mapIntersect(CalcPropertyMapImplement<I, T> implement) {
        return property.intersectFull(implement.property, implement.mapping.rightCrossValuesRev(mapping));
    }

    public ActionPropertyMapImplement<?, T> getSetNotNullAction(boolean notNull) {
        ActionPropertyMapImplement<?, P> action = property.getSetNotNullAction(notNull);
        if(action!=null)
            return action.map(mapping);
        return null;
    }
    
    public static <T extends PropertyInterface> ImCol<CalcPropertyMapImplement<?, T>> filter(ImCol<CalcPropertyInterfaceImplement<T>> col) {
        return BaseUtils.immutableCast(col.filterCol(new SFunctionSet<CalcPropertyInterfaceImplement<T>>() {
            public boolean contains(CalcPropertyInterfaceImplement<T> element) {
                return element instanceof CalcPropertyMapImplement;
            }}));
    }

    public <L> CalcPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new CalcPropertyImplement<P, L>(property, mapping.join(mapImplement));
    }

    public <L> CalcPropertyRevImplement<P, L> mapRevImplement(ImRevMap<T, L> mapImplement) {
        return new CalcPropertyRevImplement<P, L>(property, mapping.join(mapImplement));
    }

    public CalcPropertyInterfaceImplement<T> mapClassProperty() {
        return property.getClassProperty().mapPropertyImplement(mapping);
    }

    // временно
    public CalcPropertyMapImplement<?, T> cloneProp() {
        return DerivedProperty.createJoin(new CalcPropertyImplement<P, CalcPropertyInterfaceImplement<T>>(property, BaseUtils.<ImMap<P, CalcPropertyInterfaceImplement<T>>>immutableCast(mapping)));
    }
}
