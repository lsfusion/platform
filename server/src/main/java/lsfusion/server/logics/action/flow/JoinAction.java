package lsfusion.server.logics.action.flow;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.action.WatchAction;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Set;

public class JoinAction<T extends PropertyInterface> extends KeepContextAction {

    public final ActionImplement<T, PropertyInterfaceImplement<PropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinAction(LocalizedString caption, ImOrderSet<I> listInterfaces, ActionImplement<T, PropertyInterfaceImplement<I>> implement) {
        super(caption, listInterfaces.size());

        action = PropertyFact.mapActionImplements(implement, getMapInterfaces(listInterfaces).reverse());
        assert checkProps(action.mapping.values());

        finalizeInit();
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImFilterValueMap<T, ObjectValue> mvReadValues = action.mapping.mapFilterValues();
        for (int i=0,size=action.mapping.size();i<size;i++)
            mvReadValues.mapValue(i, action.mapping.getValue(i).readClasses(context));
        action.action.execute(context.override(mvReadValues.immutableValue(), action.mapping));
        return FlowResult.FINISH;
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        if(isRecursive && recursive) // recursion guard
            return AsyncMapExec.RECURSIVE();

        AsyncMapEventExec<T> simpleInput = action.action.getAsyncEventExec(optimistic, isRecursive || recursive);
        if(simpleInput != null)
            return simpleInput.mapJoin(action.mapping);
        return null;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(isRecursive) // recursion guard
            return false;

        if (type == ChangeFlowType.RETURN)
            return false;
        return super.hasFlow(type);
    }

    public ImSet<Action> getDependActions() {
        return SetFact.singleton(action.action);
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        if(isRecursive) // recursion guard
            return MapFact.EMPTY();
        return super.aspectChangeExtProps();
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        MSet<Property> used = SetFact.mSet();
        for(PropertyInterfaceImplement<PropertyInterface> value : action.mapping.valueIt())
            value.mapFillDepends(used);
        ImMap<Property, Boolean> result = used.immutable().toMap(false);
        if(!isRecursive)
            result = result.merge(super.aspectUsedExtProps(), addValue);
        return result;
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() { // тут на recursive не смо
        return PropertyFact.createJoin(action.action.getWhereProperty(true).mapImplement(action.mapping));
    }

    @Override
    public ImList<ActionMapImplement<?, PropertyInterface>> getList() {
        // если все интерфейсы однозначны и нет return'ов - inlin'им
        if(isRecursive || action.action.hasFlow(ChangeFlowType.RETURN))
            return super.getList();
        
        ImRevMap<T, PropertyInterface> identityMap = PropertyInterface.getIdentityMap(action.mapping);
        if(identityMap == null)
            return super.getList();

        return PropertyFact.mapActionImplements(identityMap, action.action.getList());
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer) {
        if(isRecursive)
            return null;

        ActionMapImplement<?, T> replacedAction = action.action.replace(replacer);
        if(replacedAction == null)
            return null;

        return PropertyFact.createJoinAction(replacedAction.map(action.mapping));
    }

    private boolean isRecursive;
    // пока исходим из того что рекурсивными могут быть только abstract'ы
    @Override
    protected void markRecursions(ImSet<ListCaseAction> recursiveActions, Set<Action> marks) {
        Action<T> execAction = action.action;
        if(execAction instanceof ListCaseAction && recursiveActions.contains((ListCaseAction)execAction)) {
            assert ((ListCaseAction) execAction).isAbstract();
            isRecursive = true;
        } else
            super.markRecursions(recursiveActions, marks);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        if(action.action instanceof WatchAction)
            return super.getDelegationType(modifyContext);
        return ActionDelegationType.IN_DELEGATE; // jump to another LSF
    }

    @Override
    protected ImSet<Pair<String, Integer>> getRecInnerDebugActions() {
        if(isRecursive) // recursion guard
            return SetFact.EMPTY();
        return super.getRecInnerDebugActions();
    }

    @Override
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        if(isRecursive) // recursion guard
            return false;
        
        return action.action.endsWithApplyAndNoChangesAfterBreaksBefore();
    }
}
