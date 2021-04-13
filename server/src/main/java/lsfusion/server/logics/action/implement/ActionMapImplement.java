package lsfusion.server.logics.action.implement;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.CaseAction;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.interactive.action.input.SimpleRequestInput;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.DebugInfo;

import java.sql.SQLException;

public class ActionMapImplement<P extends PropertyInterface, T extends PropertyInterface> implements ActionOrPropertyInterfaceImplement<T> {

    public Action<P> action;
    public ImRevMap<P, T> mapping;

    public ActionMapImplement(Action<P> action) {
        this(action, MapFact.EMPTYREV());
    }

    public ActionMapImplement(Action<P> action, ImRevMap<P, T> mapping) {
        this.action = action;
        this.mapping = mapping;
        assert BaseUtils.hashEquals(action.interfaces, mapping.keys());
    }

    public <K extends PropertyInterface> ActionMapImplement<P, K> map(ImRevMap<T, K> remap) {
        return new ActionMapImplement<>(action, mapping.join(remap));
    }

    public <L extends PropertyInterface> void mapEventAction(LogicsModule lm, PropertyMapImplement<L, T> where, Event event, boolean resolve, DebugInfo.DebugPoint debugInfo) {
        lm.addEventAction(action, where.map(mapping.reverse()), MapFact.<PropertyInterfaceImplement<P>, Boolean>EMPTYORDER(), false, event, resolve, debugInfo);
    }

    public ActionObjectEntity<P> mapObjects(ImRevMap<T, ObjectEntity> mapObjects) {
        return new ActionObjectEntity<>(action, mapping.join(mapObjects));
    }

    public PropertyMapImplement<?, T> mapWhereProperty() {
        return action.getWhereProperty().map(mapping);
    }

    public PropertyMapImplement<?, T> mapCalcWhereProperty() {
        return action.getWhereProperty(true).map(mapping);
    }

    public LA<P> createLP(ImOrderSet<T> listInterfaces) {
        return new LA<>(action, listInterfaces.mapOrder(mapping.reverse()));
    }

    public FlowResult execute(ExecutionContext<T> context) throws SQLException, SQLHandledException {
        return action.execute(context.map(mapping));
    }

    public T mapSimpleDelete() {
        P simpleDelete = action.getSimpleDelete();
        if(simpleDelete!=null)
            return mapping.get(simpleDelete);
        return null;
    }
    
    public ActionMapImplement<?, T> mapReplaceExtend(Action.ActionReplacer replacer) {
        ActionMapImplement<?, P> replaced = action.replace(replacer);
        if(replaced != null)
            return replaced.map(mapping);
        return null;        
    }

    public ImList<ActionMapImplement<?, T>> getList() {
        return PropertyFact.mapActionImplements(mapping, action.getList());
    }
/*    public ActionMapImplement<?, T> compile() {
        return property.compile().map(mapping);
    }*/
    public boolean hasPushFor(ImSet<T> context, boolean ordersNotNull) {
        return action.hasPushFor(mapping, context, ordersNotNull);
    }
    public Property getPushWhere(ImSet<T> context, boolean ordersNotNull) {
        return action.getPushWhere(mapping, context, ordersNotNull);
    }
    public ActionMapImplement<?, T> pushFor(ImSet<T> context, PropertyMapImplement<?, T> where, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        return action.pushFor(mapping, context, where, orders, ordersNotNull);
    }
    public boolean hasFlow(ChangeFlowType... types) {
        for(ChangeFlowType type : types)
            if(action.hasFlow(type))
                return true;
        return false;
    }

    public ImSet<OldProperty> mapParseOldDepends() {
        return action.getParseOldDepends();
    }

    public ActionValueImplement<P> getValueImplement(ImMap<T, ? extends ObjectValue> mapValues, ImMap<T, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance) {
        return new ActionValueImplement<>(action, mapping.join(mapValues), mapObjects != null ? mapping.innerJoin(mapObjects) : null, formInstance);
    }

    public Graph<ActionCase<T>> mapAbstractGraph() {
        if(action instanceof CaseAction) {
            Graph<ActionCase<PropertyInterface>> absGraph = ((CaseAction) action).getAbstractGraph();
            if(absGraph != null)
                return absGraph.map(value -> value.map((ImRevMap<PropertyInterface, T>) mapping));
        }
        return null;        
    }

    public boolean equalsMap(ActionOrPropertyInterfaceImplement object) {
        if(!(object instanceof ActionMapImplement))
            return false;

        ActionMapImplement<?, T> mapProp = (ActionMapImplement<?, T>) object;
        return action.equals(mapProp.action) && mapping.equals(mapProp.mapping);
    }

    public int hashMap() {
        return 31 * action.hashCode() + mapping.hashCode();
    }

    public String toString() {
        return action.toString() + " {" + mapping + "}";
    }
    
    public <X> ActionImplement<P, X> map(ImMap<T, X> map) {
        return new ActionImplement<>(action, mapping.join(map));
    }

    public SimpleRequestInput<T> mapSimpleRequestInput(boolean optimistic, boolean inRequest) {
        SimpleRequestInput<P> simpleRequestInput = action.getSimpleRequestInput(optimistic, inRequest);
        if(simpleRequestInput != null)
            return simpleRequestInput.map(mapping);
        return null;
    }
}
