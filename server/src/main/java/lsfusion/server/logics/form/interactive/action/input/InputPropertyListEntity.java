package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.AppImage;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.Collections;

public class InputPropertyListEntity<P extends PropertyInterface, V extends PropertyInterface> extends InputListEntity<P, V, Property<P>> {

    public InputPropertyListEntity(Property<P> property, ImRevMap<P, V> mapValues) {
        this(property, mapValues, false);
    }

    public InputPropertyListEntity(Property<P> property, ImRevMap<P, V> mapValues, boolean newSession) {
        super(property, mapValues, newSession);
    }

    public <X extends PropertyInterface> PropertyMapImplement<P, X> getProperty(ImRevMap<V, X> map, X objectInterface) {
        return new PropertyMapImplement<>(property, mapValues.join(map).addRevExcl(singleInterface(), objectInterface));
    }

    public ActionMapImplement<?, V> getAsyncUpdateAction(BaseLogicsModule lm, PropertyMapImplement<?, V> targetProp) {
        ImRevMap<P, PropertyInterfaceImplement<V>> mapIntValues = BaseUtils.immutableCast(mapValues);
        return PropertyFact.createIfAction(SetFact.EMPTY(), PropertyFact.createNot(lm.getRequestCanceledProperty().getImplement()), // IF NOT requestCanceled()
                PropertyFact.createJoinAction(lm.addAsyncUpdateAProp().getActionOrProperty(), // ASYNCUPDATE
                        // list(requestedProperty(), ...)
                        PropertyFact.createJoin(new PropertyImplement<>(property, MapFact.addExcl(mapIntValues, singleInterface(), targetProp)))), null);
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

        return new InputContextAction<>(AppServerImage.ADD, AppImage.INPUT_NEW, "INSERT", Collections.singletonMap("editing", BindingMode.ONLY), null, QuickAccess.EMPTY, newEdit.getActionOrProperty(),
                listInterfaces.mapSet(newEdit.listInterfaces).removeRev(singleInterface).crossJoin(mapValues));
    }

    public <J extends PropertyInterface> InputContextPropertyListEntity<?, V> merge(Pair<InputFilterEntity<?, V>, ImOrderMap<InputOrderEntity<?, V>, Boolean>> filterAndOrders) {
        if(filterAndOrders == null)
            return null;

        InputFilterEntity<?, V> filter = filterAndOrders.first;
        ImOrderMap<InputOrderEntity<?, V>, Boolean> orders = filterAndOrders.second;

        if(filter == null) { // it's not only the optimization, but also needed for the singleInterface assertion (and all the rest assertions)
            assert orders == null;
            return new InputContextPropertyListEntity<>(this);
        }

        assert singleInterface() != null;

        return new InputContextPropertyListEntity<>(this, filter, orders);
    }

    public DataClass getDataClass() {
        return (DataClass) property.getType();
    }

    public ImSet<V> getUsedInterfaces() {
        return mapValues.valuesSet();
    }

    public boolean isValueUnique(ImRevMap<V, StaticParamNullableExpr> listParamExprs) {
        if(!Property.isDefaultWYSInput(property.getValueClass(ClassType.typePolicy)))
            return false;

        ImMap<P, StaticParamNullableExpr> fixedExprs = mapValues.join(listParamExprs);
        return property.isValueUnique(fixedExprs, Property.ValueUniqueType.INPUT);
    }

    public <C extends PropertyInterface> InputPropertyListEntity<P, C> mapProperty(ImRevMap<V, C> map) {
        return map(map);
    }

    public InputPropertyListEntity<P, V> newSession() {
        return new InputPropertyListEntity<>(property, mapValues, true);
    }

    protected  <C extends PropertyInterface> InputPropertyListEntity<P, C> create(ImRevMap<P, C> joinMapValues) {
        return new InputPropertyListEntity<>(property, joinMapValues, newSession);
    }

    public <C extends PropertyInterface> InputPropertyListEntity<JoinProperty.Interface, C> createJoin(ImMap<V, PropertyInterfaceImplement<C>> mappedValues) {
        Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, C>> joinImplement = PropertyFact.createPartJoin(new PropertyImplement<>(property, mapValues.join(mappedValues)));
        return new InputPropertyListEntity<>(joinImplement.first, joinImplement.second, newSession);
    }

    public InputPropertyValueList<?> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects) {
        return new InputPropertyValueList<>(property.interfaces, property.getImplement(), MapFact.EMPTYORDER(), (ImMap<P, ObjectValue>) mapValues.join(map), mapValues.join(mapObjects));
    }

    @Override
    public <C extends PropertyInterface> InputPropertyListEntity<P, C> map(ImRevMap<V, C> map) {
        return (InputPropertyListEntity<P, C>) super.map(map);
    }
}
