package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.Collections;

import static lsfusion.server.logics.property.oraction.PropertyInterface.getIdentityMap;

// pretty similar to ContextFilterEntity
public class InputListEntity<P extends PropertyInterface, V extends PropertyInterface> {

    private final Property<P> property;

    private final ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders;

    private final ImRevMap<P, V> mapValues; // external context

    public final boolean newSession;

    public InputListEntity(Property<P> property, ImRevMap<P, V> mapValues) {
        this(property, MapFact.EMPTYORDER(), mapValues, false);
    }
    private InputListEntity(Property<P> property, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, ImRevMap<P, V> mapValues, boolean newSession) {
        this.property = property;
        this.orders = orders;
        this.mapValues = mapValues;
        this.newSession = newSession;

        assert property.interfaces.containsAll(mapValues.keys());
    }

    public P singleInterface() {
        return getInterfaces().single();
    }

    public ImSet<P> getInterfaces() {
        return property.interfaces.removeIncl(mapValues.keys());
    }

    public Property<P> getProperty() {
        return property;
    }

    public InputListEntity<P, V> newSession() {
        return new InputListEntity<>(property, orders, mapValues, true);
    }

    public <C extends PropertyInterface> InputListEntity<P, C> map(ImRevMap<V, C> map) {
        return new InputListEntity<P, C>(property, orders, mapValues.join(map), newSession);
    }

    public <C extends PropertyInterface> InputListEntity<P, C> mapInner(ImRevMap<V, C> map) {
        // here it's not evident if we should consider the case like FOR f=g(a) DO INPUT ... LIST x(d) IF g(d) = f as a simple input
        // we won't since we don't do that in FilterEntity, ContextFilterEntity.getInputListEntity
        ImRevMap<P, C> joinMapValues = mapValues.innerJoin(map);
        if(joinMapValues.size() != mapValues.size())
            return null;
        return new InputListEntity<>(property, orders, joinMapValues, newSession);
    }

    public <C extends PropertyInterface> InputListEntity<?, C> mapJoin(ImMap<V, PropertyInterfaceImplement<C>> mapping) {
        ImMap<P, PropertyInterfaceImplement<C>> mappedValues = mapValues.join(mapping);

        ImRevMap<P, C> identityMap = getIdentityMap(mappedValues);
        if(identityMap != null) // optimization + fix of not empty orders
            return new InputListEntity<P, C>(property, orders, identityMap, newSession);

        Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, C>> joinImplement = PropertyFact.createPartJoin(new PropertyImplement<>(property, mappedValues));
        return new InputListEntity<>(joinImplement.first, MapFact.EMPTYORDER(), joinImplement.second, newSession);
    }

    public InputValueList<P> map(ImMap<V, ? extends ObjectValue> map) {
        return new InputValueList<>(property, orders, BaseUtils.immutableCast(mapValues.join(map)));
    }

    public <J extends PropertyInterface, X extends PropertyInterface> InputListEntity<?, V> merge(Pair<InputFilterEntity<?, V>, ImOrderMap<InputOrderEntity<?, V>, Boolean>> filterAndOrders) {
        if(filterAndOrders == null)
            return null;

        InputFilterEntity<?, V> filter = filterAndOrders.first;
        ImOrderMap<InputOrderEntity<?, V>, Boolean> orders = filterAndOrders.second;

        if(filter == null && orders.isEmpty()) // it's not only the optimization, but also needed for the singleInterface assertion (and all the rest assertions)
            return this;

        assert singleInterface() != null;

        InputFilterEntity<J, V> mergeFilter = (InputFilterEntity<J, V>) InputFilterEntity.and(new InputFilterEntity<>(property, mapValues), filter);

        return new InputListEntity<>(mergeFilter.property, mergeOrders(mergeFilter, orders), mergeFilter.mapValues, newSession);
    }

    private static <J extends PropertyInterface, X extends PropertyInterface, V extends PropertyInterface> ImOrderMap<PropertyInterfaceImplement<J>, Boolean> mergeOrders(InputFilterEntity<J, V> mergeFilter, ImOrderMap<InputOrderEntity<?, V>, Boolean> orders) {
        MOrderExclMap<PropertyInterfaceImplement<J>, Boolean> mMergeOrders = MapFact.mOrderExclMapMax(orders.size());
        for(int i = 0, size = orders.size(); i < size; i++) {
            InputOrderEntity<X, V> order = (InputOrderEntity<X, V>) orders.getKey(i);
            ImRevMap<X, J> mappedOrder = order.mapValues.innerCrossValues(mergeFilter.mapValues);
            // we'll have to patch with params if there are some extra params in order - to keep this structure: Property<P> and map to its params
            // todo: but for now we'll just ignore such orders
            if(mappedOrder.size() == order.mapValues.size())
                mMergeOrders.exclAdd(new PropertyMapImplement<>(order.property, MapFact.addRevExcl(mappedOrder, order.singleInterface(), mergeFilter.singleInterface())), orders.getValue(i));
        }
        ImOrderMap<PropertyInterfaceImplement<J>, Boolean> mergeOrders = mMergeOrders.immutableOrder();

        Stat interfaceStat = mergeFilter.property.getInterfaceStat(mergeFilter.mapValues.keys());
        boolean tooManyRows = interfaceStat.getCount() > Settings.get().getAsyncValuesMaxReadOrderCount();

        // we could do it in getListExpr (at the bottom stack point),
        // but it seems better to do it here (at the top stack point), to properly handle caches ("global read" caches in particular)
        // the check is that when we have too much rows, we remove the order for the optimization purposes
        if(!mergeOrders.isEmpty()) {
            if(tooManyRows)
                mergeOrders = MapFact.EMPTYORDER();
        } else {
            if(!tooManyRows)
                mergeOrders = MapFact.singletonOrder(mergeFilter.singleInterface(), false);
        }
        return mergeOrders;
    }

    public DataClass getDataClass() {
        return (DataClass) property.getType();
    }

    public ImMap<V, ValueClass> getInterfaceClasses() {
        return mapValues.innerCrossJoin(property.getInterfaceClasses(ClassType.wherePolicy));
    }

    public static <X extends PropertyInterface, V extends PropertyInterface> InputContextAction<?, V> getResetAction(BaseLogicsModule baseLM, LP targetProp) {
        assert targetProp.listInterfaces.isEmpty();
        LA<X> reset = (LA<X>) baseLM.addResetAProp(targetProp);
        return new InputContextAction<>("reset", QuickAccess.DEFAULT, reset.getActionOrProperty(), MapFact.EMPTYREV());
    }

    public <X extends PropertyInterface> InputContextAction<?, V> getNewEditAction(BaseLogicsModule baseLM, ConcreteCustomClass baseClass, LP targetProp, FormSessionScope scope) {
        LP<P> lp = new LP<>(property);
        ImOrderSet<P> listInterfaces = lp.listInterfaces;
        P singleInterface = singleInterface();

        int contextParams = property.interfaces.size();
        int singleIndex = listInterfaces.indexOf(singleInterface) + 1; // object interface, have to be replaced with newIndex, and in set action it's gonna be a string interface

        LA<X> newEdit = (LA<X>) baseLM.addNewEditAction(baseClass, targetProp, contextParams, scope,
                BaseUtils.add(BaseUtils.add(lp, ActionOrPropertyUtils.getIntParams(lp, singleIndex, contextParams + 1)), // remapping single interface to the new object
                        singleIndex)); // replacing property with the string

        return new InputContextAction<>("new", "INSERT", Collections.singletonMap("editing", BindingMode.ONLY), null, QuickAccess.EMPTY, newEdit.getActionOrProperty(),
                listInterfaces.mapSet(newEdit.listInterfaces).removeRev(singleInterface).crossJoin(mapValues));
    }

    public ObjectValue readObject(ExecutionContext<V> context, ObjectValue userValue) throws SQLException, SQLHandledException {
        return FormInstance.getAsyncKey(map(context.getKeys()), context.getSession(), context.getModifier(), userValue);
    }

    public ActionMapImplement<?, V> getAsyncUpdateAction(BaseLogicsModule lm, PropertyMapImplement<?, V> targetProp) {
        ImRevMap<P, PropertyInterfaceImplement<V>> mapIntValues = BaseUtils.immutableCast(mapValues);
        return PropertyFact.createIfAction(SetFact.EMPTY(), PropertyFact.createNot(lm.getRequestCanceledProperty().getImplement()), // IF NOT requestCanceled()
                PropertyFact.createJoinAction(lm.addAsyncUpdateAProp().getActionOrProperty(), // ASYNCUPDATE
                        // list(requestedProperty(), ...)
                        PropertyFact.createJoin(new PropertyImplement<>(property, MapFact.addExcl(mapIntValues, singleInterface(), targetProp)))), null);
    }

    public ImSet<V> getUsedInterfaces() {
        return mapValues.valuesSet();
    }
    
    public boolean isDefaultWYSInput() {
        return Property.isDefaultWYSInput(property.getValueClass(ClassType.tryEditPolicy));
    }
    
    public boolean isValueUnique(ImRevMap<V, StaticParamNullableExpr> listParamExprs) {
        ImRevMap<P, StaticParamNullableExpr> mapExprs = mapValues.join(listParamExprs);
        return property.isValueFull(mapExprs) && property.isValueUnique(mapExprs, true);
    }
    
    @Override
    public String toString() {
        return property + "(" + mapValues + ")" + (newSession ? " NEW" : "");
    }
}
