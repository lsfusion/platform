package lsfusion.server.logics.property.implement;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.CalcDataType;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends PropertyRevImplement<P, T> implements PropertyInterfaceImplement<T> {

    public PropertyMapImplement(Property<P> property) {
        super(property, MapFact.EMPTYREV());
    }
    
    public PropertyMapImplement(Property<P> property, ImRevMap<P, T> mapping) {
        super(property, mapping);
    }

    public DataChanges mapJoinDataChanges(PropertyChange<T> change, CalcDataType type, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges) {
        ImMap<T, Expr> mapExprs = change.getMapExprs();
        if(mapExprs.size() == mapping.size()) // optimization
            return property.getDataChanges(change.mapChange(mapping), type, propChanges, changedWhere);
        return property.getJoinDataChanges(mapping.join(mapExprs), change.expr, change.where, groupType, propChanges, type, changedWhere);
    }

    public PropertyMapImplement<P, T> mapOld(PrevScope event) {
        return new PropertyMapImplement<>(property.getOld(event), mapping);
    }

    public PropertyMapImplement<P, T> mapChanged(IncrementType type, PrevScope scope) {
        return new PropertyMapImplement<>(property.getChanged(type, scope), mapping);
    }

    public PropertyValueImplement<P> mapValues(ImMap<T, DataObject> mapValues) {
        return new PropertyValueImplement<>(property, mapping.join(mapValues));
    }

    public PropertyValueImplement<P> mapObjectValues(ImMap<T, ? extends ObjectValue> mapValues) {
        ImMap<P, ? extends ObjectValue> mapped = mapping.join(mapValues);
        ImMap<P, DataObject> mappedData = DataObject.filterDataObjects(mapped);
        if(mappedData.size() < mapped.size())
            return null;
        return new PropertyValueImplement<>(property, mappedData);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException, SQLHandledException {
        change(keys, env, env.getSession().getObjectValue(property.getValueClass(ClassType.editValuePolicy), value));
    }

    public <K extends PropertyInterface> PropertyMapImplement<P, K> map(ImRevMap<T, K> remap) {
        return new PropertyMapImplement<>(property, mapping.join(remap));
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, ObjectValue objectValue) throws SQLException, SQLHandledException {
        env.change(property, mapValues(keys).getPropertyChange(objectValue.getExpr()));
    }

    public void change(ExecutionEnvironment env, PropertyChange<T> change) throws SQLException, SQLHandledException {
        env.change(property, change.mapChange(mapping));
    }

    public ImMap<T,ValueClass> mapInterfaceClasses(ClassType type) {
        return mapInterfaceClasses(type, null);
    }

    public ImMap<T,ValueClass> mapInterfaceClasses(ClassType type, ExClassSet valueClasses) {
        return mapping.rightCrossJoin(property.getInterfaceClasses(type, valueClasses));
    }

    public ClassWhere<T> mapClassWhere(ClassType type) {
        return new ClassWhere<>(property.getClassWhere(type), mapping);
    }

    public boolean mapIsInInterface(ImMap<T, ? extends AndClassSet> classes, boolean isAny) {
        return property.isInInterface(mapping.join(classes), isAny);
    }

    public ImMap<T, ValueClass> mapGetInterfaceClasses(ClassType classType) {
        return mapping.rightCrossJoin(property.getInterfaceClasses(classType));
    }

    public boolean mapIsNotNull(ImSet<T> interfaces) {
        if(interfaces.isEmpty()) // оптимизация
            return true;

        ImSet<P> checkInterfaces = mapping.filterValues(interfaces).keys();

        // если все собрали интерфейсы
        return checkInterfaces.size() >= interfaces.size() && property.isNotNull(checkInterfaces, AlgType.actionType);
    }

    public boolean mapIsFull(ImSet<T> interfaces) {
        if(interfaces.isEmpty()) // оптимизация
            return true;

        ImSet<P> checkInterfaces = mapping.filterValues(interfaces).keys();

        // если все собрали интерфейсы
        return checkInterfaces.size() >= interfaces.size() && property.isFull(checkInterfaces, AlgType.actionType);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) throws SQLException, SQLHandledException {
        return property.getExpr(mapping.join(joinImplement), modifier);
    }
    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return property.getExpr(mapping.join(joinImplement), propChanges);
    }

    public void mapFillDepends(MSet<Property> depends) {
        depends.add(property);
    }

    public int mapHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
    public boolean mapEquals(PropertyInterfaceImplement<T> implement) {
        return implement instanceof PropertyMapImplement && property.equals(((PropertyMapImplement) implement).property) && mapping.equals(((PropertyMapImplement) implement).mapping);
    }

    public ImSet<OldProperty> mapOldDepends() {
        return property.getOldDepends();
    }

    public Object read(ExecutionContext context, ImMap<T, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException {
        return property.read(context.getSession().sql, mapping.join(interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    public ObjectValue readClasses(ExecutionContext context, ImMap<T, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException {
        return property.readClasses(context.getSession(), mapping.join(interfaceValues), context.getModifier(), context.getQueryEnv());
    }

    @Override
    public boolean mapHasAlotKeys() {
        return property.hasAlotKeys();
    }

    @Override
    public int mapEstComplexity() {
        return property.getEstComplexity();
    }

    public ImSet<DataProperty> mapChangeProps() {
        return property.getChangeProps();
    }

    public PropertyMapImplement<?, T> mapIdentityImplement() {
        return property.getIdentityImplement(mapping);
    }

    public boolean mapHasPreread(PropertyChanges propertyChanges) {
        return property.hasPreread(propertyChanges);
    }
    public boolean mapHasPreread(Modifier modifier) throws SQLException, SQLHandledException {
        return property.hasPreread(modifier);
    }

    public long mapSimpleComplexity() {
        return property.getSimpleComplexity();
    }

    public DataChanges mapJoinDataChanges(ImMap<T, ? extends Expr> mapKeys, Expr expr, Where where, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges, CalcDataType type) {
        return property.getJoinDataChanges(mapping.join(mapKeys), expr, where, groupType, propChanges, type, changedWhere);
    }

    public void fill(MSet<T> interfaces, MSet<PropertyMapImplement<?, T>> properties) {
        properties.add(this);
    }

    public ImSet<T> getInterfaces() {
        return mapping.valuesSet();
    }

    @Override
    public ActionMapImplement<?, T> mapEventAction(String eventSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        ActionMapImplement<?, P> eventAction = property.getEventAction(eventSID, defaultChangeEventScope, viewProperties, customChangeFunction);
        return eventAction == null ? null : eventAction.map(mapping);
    }

    public Inferred<T> mapInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return property.inferInterfaceClasses(commonValue, inferType).map(mapping);
    }
    public boolean mapNeedInferredForValueClass(InferType inferType) {
        return property.needInferredForValueClass(inferType);
    }
    public ExClassSet mapInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        return property.inferValueClass(mapping.join(inferred), inferType);
    }

    public AndClassSet mapValueClassSet(ClassWhere<T> interfaceClasses) {
        return property.getValueClassSet();
    }

    public PropertyObjectInstance<P> mapObjects(ImMap<T, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new PropertyObjectInstance<>(property, mapping.join(mapObjects));
    }

    public PropertyObjectEntity<P> mapEntityObjects(ImRevMap<T, ObjectEntity> mapObjects) {
        return new PropertyObjectEntity<>(property, mapping.join(mapObjects));
    }

    public <I extends PropertyInterface> void mapCheckExclusiveness(String caseInfo, PropertyMapImplement<I, T> implement, String implementCaption, String abstractInfo) {
        property.checkExclusiveness(caseInfo, implement.property, implementCaption, implement.mapping.rightCrossValuesRev(mapping), abstractInfo);
    }

    public ActionMapImplement<?, T> getSetNotNullAction(boolean notNull) {
        ActionMapImplement<?, P> action = property.getSetNotNullAction(notNull);
        if(action!=null)
            return action.map(mapping);
        return null;
    }
    
    public static <T extends PropertyInterface> ImCol<PropertyMapImplement<?, T>> filter(ImCol<PropertyInterfaceImplement<T>> col) {
        return BaseUtils.immutableCast(col.filterCol(element -> element instanceof PropertyMapImplement));
    }

    public <L> PropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new PropertyImplement<>(property, mapping.join(mapImplement));
    }

    public <L> PropertyRevImplement<P, L> mapRevImplement(ImRevMap<T, L> mapImplement) {
        return new PropertyRevImplement<>(property, mapping.join(mapImplement));
    }

    public PropertyMapImplement<?, T> mapClassProperty() {
        return property.getClassProperty().mapPropertyImplement(mapping);
    }

    // временно
    public PropertyMapImplement<?, T> cloneProp() {
        return PropertyFact.createJoin(new PropertyImplement<>(property, BaseUtils.immutableCast(mapping)));
    }

    public Graph<CalcCase<T>> mapAbstractGraph() {
        if(property instanceof CaseUnionProperty) {
            Graph<CalcCase<UnionProperty.Interface>> absGraph = ((CaseUnionProperty) property).abstractGraph;
            if(absGraph != null)
                return absGraph.map(value -> value.map((ImRevMap<UnionProperty.Interface, T>) mapping));
        }
        return null;
    }
    
    public boolean equalsMap(ActionOrPropertyInterfaceImplement object) {
        if(!(object instanceof PropertyMapImplement))
            return false;

        PropertyMapImplement<?, T> mapProp = (PropertyMapImplement<?, T>) object;
        return property.equals(mapProp.property) && mapping.equals(mapProp.mapping);
    }

    public int hashMap() {
        return 31 * property.hashCode() + mapping.hashCode();
    }
}
