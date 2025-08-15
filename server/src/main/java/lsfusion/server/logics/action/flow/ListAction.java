package lsfusion.server.logics.action.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFListImpl;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

public class ListAction extends ListCaseAction {

    private Object actions;
    public void addAction(ActionMapImplement<?, PropertyInterface> action, Version version) {
        assert isAbstract();

        assert action != null;
        NFListImpl.add(isLast, (NFList<ActionMapImplement<?, PropertyInterface>>) actions, action, version);

        addWhereOperand(action, null, version);
    }

    public ImList<ActionMapImplement<?, PropertyInterface>> getActions() {
        return (ImList<ActionMapImplement<?, PropertyInterface>>)actions;
    }

    private final ImSet<SessionDataProperty> localsInScope;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ImList<ActionMapImplement<?, I>> actions, ImSet<SessionDataProperty> localsInScope)  {
        super(caption, false, innerInterfaces);

        ImList<ActionMapImplement<?, PropertyInterface>> mappedActions = PropertyFact.mapActionImplements(getMapInterfaces(innerInterfaces).reverse(), actions);
        this.actions = mappedActions;
        assert checkActions(mappedActions.getCol());
        this.localsInScope = localsInScope;

        finalizeInit();
    }

    // abstract конструктор без finalize'а
    public <I extends PropertyInterface> ListAction(LocalizedString caption, boolean isChecked, boolean isLast, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, false, isChecked, isLast, AbstractType.LIST, innerInterfaces, mapClasses);

        actions = NFFact.list();
        localsInScope = SetFact.EMPTY();
    }

    public PropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty() {

        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres = getActions().mapListValues((Function<ActionMapImplement<?, PropertyInterface>, PropertyInterfaceImplement<PropertyInterface>>) ActionMapImplement::mapCalcWhereProperty);
        return PropertyFact.createUnion(interfaces, listWheres);
    }

    protected ImList<ActionMapImplement<?, PropertyInterface>> getListActions() {
        return getActions();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;
        
        int lastHasSessionUsages = 0;
        if(!context.hasMoreSessionUsages && hasFlow(ChangeFlowType.NEEDMORESESSIONUSAGES)) // optimization
            lastHasSessionUsages = getLastHasSessionUsages();

        ImList<ActionMapImplement<?, PropertyInterface>> actions = getActions();
        for (int i=0,size=actions.size();i<size;i++) {
            FlowResult actionResult = actions.get(i).execute(i < lastHasSessionUsages ? context.override(true) : context);
            if (!actionResult.isFinish()) {
                result =  actionResult;
                break;
            }
        }

        context.getSession().dropSessionChanges(localsInScope);

        return result;
    }
    
    private int getLastHasSessionUsages() {
        ImList<ActionMapImplement<?, PropertyInterface>> actions = getActions();
        for(int i=actions.size()-1;i>=0;i--)
            if(actions.get(i).hasFlow(ChangeFlowType.HASSESSIONUSAGES))
                return i;            
        return 0;
    }

    @Override
    protected void finalizeAbstractInit() {
        super.finalizeAbstractInit();
        
        actions = ((NFList<ActionMapImplement<?, PropertyInterface>>)actions).getList();
    }

    @Override
    public ImList<ActionMapImplement<?, PropertyInterface>> getList(ImSet<Action<?>> recursiveAbstracts) {
        if (isRecursive) {
            if (recursiveAbstracts.contains(this)) return super.getList(recursiveAbstracts);
            recursiveAbstracts = recursiveAbstracts.addExcl(this);
        }
        
        MList<ActionMapImplement<?, PropertyInterface>> mResult = ListFact.mList();
        for(ActionMapImplement<?, PropertyInterface> action : getActions())
            mResult.addAll(action.getList(recursiveAbstracts));
        return mResult.immutableList();
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        if (isRecursive) {
            if (recursiveAbstracts.contains(this)) return null;
            recursiveAbstracts = recursiveAbstracts.addExcl(this);
        }
        final ImSet<Action<?>> localRecursiveAbstracts = recursiveAbstracts;
        
        ImList<ActionMapImplement<?, PropertyInterface>> list = getList(SetFact.EMPTY());
        ImList<ActionMapImplement<?, PropertyInterface>> replacedList = list.mapListValues((ActionMapImplement<?, PropertyInterface> element) -> element.mapReplaceExtend(replacer, localRecursiveAbstracts));

        if(replacedList.filterList(Objects::nonNull).isEmpty())
            return null;

        return PropertyFact.createListAction(interfaces, replacedList.mapListValues((i, element) -> {
            if(element == null)
                return list.get(i);
            return element;
        }), localsInScope);
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, ImSet<Action<?>> recursiveAbstracts) {
        if (isRecursive) {
            if (recursiveAbstracts.contains(this)) return AsyncMapExec.RECURSIVE();
            recursiveAbstracts = recursiveAbstracts.addExcl(this);
        }
        
        return getListAsyncEventExec(getListActions(), optimistic, recursiveAbstracts);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        if(modifyContext || hasDebugLocals())
            return ActionDelegationType.BEFORE_DELEGATE;
        return null;
    }

    @Override
    @IdentityStartLazy
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore(FormChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if (isRecursive) {
            if (recursiveAbstracts.contains(this)) return false;
            recursiveAbstracts = recursiveAbstracts.addExcl(this);
        }
        
        boolean lookingForChangeFlow = false;
        boolean lookingForChange = true;
        ImList<ActionMapImplement<?, PropertyInterface>> actions = getActions();
        for(int i = actions.size() - 1; i>= 0; i--) {
            Action<?> listAction = actions.get(i).action;
            
            if(lookingForChangeFlow && (listAction.hasFlow(ChangeFlowType.BREAK) || listAction.hasFlow(ChangeFlowType.CONTINUE) || listAction.hasFlow(ChangeFlowType.RETURN)))
                return false;

            boolean endsWithApply = listAction.endsWithApplyAndNoChangesAfterBreaksBefore(type, recursiveAbstracts);
            if(endsWithApply) {
                lookingForChange = false; // change'и уже не важны, только возможность уйти за пределы APPLY
                lookingForChangeFlow = true;
            }
            
            if(lookingForChange && listAction.hasFlow(type))
                return false;
        }
        
        return true;
    }
    
    @Override
    public ImMap<Property, Boolean> calculateUsedExtProps(ImSet<Action<?>> recursiveAbstracts) {
        if (isRecursive) {
            if (recursiveAbstracts.contains(this)) return MapFact.EMPTY();
            recursiveAbstracts = recursiveAbstracts.addExcl(this);
        }
        return super.calculateUsedExtProps(recursiveAbstracts);
    }
    
}
