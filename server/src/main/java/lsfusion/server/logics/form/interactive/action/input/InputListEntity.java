package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// pretty similar to ContextFilterEntity
public class InputListEntity<P extends PropertyInterface, V extends PropertyInterface> {

    private final Property<P> property;

    private final ImRevMap<P, V> mapValues; // external context

    public final boolean newSession;

    public InputListEntity(Property<P> property, ImRevMap<P, V> mapValues, boolean newSession) {
        this.property = property;
        this.mapValues = mapValues;
        this.newSession = newSession;

        assert property.interfaces.containsAll(mapValues.keys());
    }

    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public InputListEntity<P, V> newSession() {
        return new InputListEntity<>(property, mapValues, true);
    }

    public <C extends PropertyInterface> InputListEntity<P, C> map(ImRevMap<V, C> map) {
        return new InputListEntity<P, C>(property, mapValues.join(map), newSession);
    }

    public <C extends PropertyInterface> InputListEntity<P, C> mapInner(ImRevMap<V, C> map) {
        // here it's not evident if we should consider the case like FOR f=g(a) DO INPUT ... LIST x(d) IF g(d) = f as a simple input
        // we won't since we don't do that in FilterEntity, ContextFilterEntity.getInputListEntity
        ImRevMap<P, C> joinMapValues = mapValues.innerJoin(map);
        if(joinMapValues.size() != mapValues.size())
            return null;
        return new InputListEntity<>(property, joinMapValues, newSession);
    }

    public <C extends PropertyInterface> InputListEntity<?, C> mapJoin(ImMap<V, PropertyInterfaceImplement<C>> mapping) {
        Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, C>> joinImplement = PropertyFact.createPartJoin(new PropertyImplement<>(property, mapValues.join(mapping)));
        return new InputListEntity<>(joinImplement.first, joinImplement.second, newSession);
    }

    public InputValueList<P> map(ImMap<V, ? extends ObjectValue> map) {
        return new InputValueList<P>(property, BaseUtils.immutableCast(mapValues.join(map)));
    }

    public static <P1 extends PropertyInterface, P2 extends PropertyInterface, V extends PropertyInterface, X extends PropertyInterface>
                    InputListEntity<?, V> and(InputListEntity<P1, V> il1, InputListEntity<P2, V> il2) {
        if(il2 == null)
            return il1;
        if(il1 == null)
            return il2;

        P1 p1 = il1.singleInterface();
        P2 p2 = il2.singleInterface();

        assert !il1.newSession && !il2.newSession;
        ImRevMap<P1, P2> matchedParams = il1.mapValues.innerCrossValues(il2.mapValues).addRevExcl(MapFact.singletonRev(p1, p2));

        ImRevMap<P1, JoinProperty.Interface> firstJoinParams = il1.property.interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> rightSecondParams = il2.property.interfaces.removeIncl(matchedParams.valuesSet()).mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> secondJoinParams = rightSecondParams.addRevExcl(matchedParams.innerCrossJoin(firstJoinParams));

        ImRevMap<V, JoinProperty.Interface> mapJoinValues = il1.mapValues.crossJoin(firstJoinParams).addRevExcl(il2.mapValues.innerCrossJoin(rightSecondParams));

        PropertyMapImplement<X, JoinProperty.Interface> andProperty = (PropertyMapImplement<X, JoinProperty.Interface>)
                PropertyFact.createAnd(firstJoinParams.valuesSet().addExcl(rightSecondParams.valuesSet()),
                    new PropertyMapImplement<>(il1.property, firstJoinParams),
                    new PropertyMapImplement<>(il2.property, secondJoinParams));

        return new InputListEntity<>(andProperty.property, andProperty.mapping.innerCrossValues(mapJoinValues), false);
    }

    public <X extends PropertyInterface, J extends PropertyInterface> InputListEntity<?, V> getView(Property<X> viewProperty) {
        PropertyMapImplement<J, P> and = (PropertyMapImplement<J, P>) PropertyFact.createAnd(property.interfaces, new PropertyMapImplement<X, P>(viewProperty, MapFact.singletonRev(viewProperty.interfaces.single(), singleInterface())), property.getImplement());
        return new InputListEntity<>(and.property, and.mapping.innerJoin(mapValues), newSession);
    }

    public Pair<ActionMapImplement<?, V>, ActionMapImplement<?, V>> getActions(BaseLogicsModule lm, LP<?> targetProp) {

        DataClass dataClass = (DataClass)property.getType();
        LP<?> inputWYSProp = lm.getRequestedValueProperty().getLCP(dataClass);

        ImOrderSet<V> orderInterfaces = mapValues.valuesSet().toOrderSet();
        P singleInterface = singleInterface();
        return new Pair<>(
            //      INPUT viewProperty.getType LIST viewList(x, sdss) TO inputWYSProp
            lm.addInputAProp(dataClass, inputWYSProp.property, false, orderInterfaces, this).getImplement(orderInterfaces),
            //      FOR
            PropertyFact.createForAction(property.interfaces, mapValues.keys(),
                //      viewList(x, sdss) = requestedProperty() DO
                PropertyFact.createCompare(property.interfaces, property.getImplement(), inputWYSProp.getImplement(), Compare.EQUALS), MapFact.EMPTYORDER(), false,
                //          targetProp() <- x;
                    PropertyFact.createSetAction(SetFact.singleton(singleInterface), targetProp.getImplement(), singleInterface),
                //          ELSE targetProp() <- NULL;
                    PropertyFact.createSetAction(SetFact.EMPTY(), targetProp.getImplement(), lm.vnull.getImplement()),
                    false, SetFact.EMPTY(), false).
                        map(mapValues));
    }

    public ImMap<V, ValueClass> getInterfaceClasses() {
        return mapValues.innerCrossJoin(property.getInterfaceClasses(ClassType.wherePolicy));
    }
}
