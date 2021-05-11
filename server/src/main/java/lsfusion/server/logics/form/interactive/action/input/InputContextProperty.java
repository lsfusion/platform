package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputContextProperty<P extends PropertyInterface, V extends PropertyInterface> {
    
    public final Property<P> property;
    
    public final ImRevMap<P, V> mapValues; // external context

    public InputContextProperty(Property<P> property, ImRevMap<P, V> mapValues) {
        this.property = property;

        this.mapValues = mapValues;
        assert singleInterface() != null;
    }

    public static <P1 extends PropertyInterface, P2 extends PropertyInterface, V extends PropertyInterface, X extends PropertyInterface>
        InputContextProperty<?, V> and(InputContextProperty<P1, V> il1, InputContextProperty<P2, V> il2) {
        if(il2 == null)
            return il1;
        if(il1 == null)
            return il2;

        P1 p1 = il1.singleInterface();
        P2 p2 = il2.singleInterface();

        ImRevMap<P1, P2> matchedParams = il1.mapValues.innerCrossValues(il2.mapValues).addRevExcl(MapFact.singletonRev(p1, p2));

        ImRevMap<P1, JoinProperty.Interface> firstJoinParams = il1.property.interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> rightSecondParams = il2.property.interfaces.removeIncl(matchedParams.valuesSet()).mapRevValues(JoinProperty.genInterface);
        ImRevMap<P2, JoinProperty.Interface> secondJoinParams = rightSecondParams.addRevExcl(matchedParams.innerCrossJoin(firstJoinParams));

        ImRevMap<V, JoinProperty.Interface> mapJoinValues = il1.mapValues.crossJoin(firstJoinParams).addRevExcl(il2.mapValues.innerCrossJoin(rightSecondParams));

        PropertyMapImplement<X, JoinProperty.Interface> andProperty = (PropertyMapImplement<X, JoinProperty.Interface>)
                PropertyFact.createAnd(firstJoinParams.valuesSet().addExcl(rightSecondParams.valuesSet()),
                        new PropertyMapImplement<>(il1.property, firstJoinParams),
                        new PropertyMapImplement<>(il2.property, secondJoinParams));

        return new InputContextProperty<>(andProperty.property, andProperty.mapping.innerCrossValues(mapJoinValues));
    }
    
    public InputListEntity<P, V> getListEntity(boolean newSession) {
        return new InputListEntity<>(property, mapValues, newSession);
    }

    // input value
    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public ActionMapImplement<?, V> getDefaultAsyncUpdateAction(BaseLogicsModule lm, PropertyMapImplement<?, V> targetProp) {
        ImRevMap<P, PropertyInterfaceImplement<V>> mapIntValues = BaseUtils.immutableCast(mapValues);
        return PropertyFact.createIfAction(SetFact.EMPTY(), PropertyFact.createNot(lm.getRequestCanceledProperty().getImplement()), // IF NOT requestCanceled()
                PropertyFact.createJoinAction(lm.addAsyncUpdateAProp().getActionOrProperty(), // ASYNCUPDATE
                        // list(requestedProperty(), ...)
                        PropertyFact.createJoin(new PropertyImplement<>(property, MapFact.addExcl(mapIntValues, singleInterface(), targetProp)))), null);
    }

    public <O extends ObjectSelector> ContextFilterEntity<P, V, O> getFilter(O object) {
        return new ContextFilterEntity<>(property, mapValues, MapFact.singletonRev(singleInterface(), object));
    }

    public ImSet<V> getUsedInterfaces() {
        return mapValues.valuesSet();
    }
}
