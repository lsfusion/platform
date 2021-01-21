package lsfusion.server.logics.action.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.interop.form.property.OpenForm;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFListImpl;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
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
            if (actionResult != FlowResult.FINISH) {
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
    public ImList<ActionMapImplement<?, PropertyInterface>> getList() {
        MList<ActionMapImplement<?, PropertyInterface>> mResult = ListFact.mList();
        for(ActionMapImplement<?, PropertyInterface> action : getActions())
            mResult.addAll(action.getList());
        return mResult.immutableList();
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer) {
        ImList<ActionMapImplement<?, PropertyInterface>> list = getList();
        ImList<ActionMapImplement<?, PropertyInterface>> replacedList = list.mapListValues((ActionMapImplement<?, PropertyInterface> element) -> element.mapReplaceExtend(replacer));

        if(replacedList.filterList(Objects::nonNull).isEmpty())
            return null;

        return PropertyFact.createListAction(interfaces, replacedList.mapListValues((i, element) -> {
            if(element == null)
                return list.get(i);
            return element;
        }), localsInScope);
    }

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type type = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            Type actionRequestType = action.action.getSimpleRequestInputType(optimistic, inRequest);
            if (actionRequestType != null) {
                if (type == null) {
                    type = actionRequestType;
                } else {
                    type = type.getCompatible(actionRequestType);
                    if (type == null) {
                        return null;
                    }
                }
            }
        }
        return type;
    }

    @Override
    public CustomClass getSimpleAdd() {
        CustomClass result = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            CustomClass simpleAdd = action.action.getSimpleAdd();
            if (simpleAdd != null) {
                if (result == null) {
                    result = simpleAdd;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        PropertyInterface result = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            PropertyInterface simpleDelete = action.mapSimpleDelete();
            if (simpleDelete != null) {
                if (result == null) {
                    result = simpleDelete;
                } else {
                    result = null;
                    break;
                }
            }
        }
        if(result != null)
            return result;
        return super.getSimpleDelete();
    }

    @Override
    public OpenForm getOpenForm() {
        OpenForm result = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            OpenForm openForm = action.action.getOpenForm();
            if (openForm != null) {
                if (result == null) {
                    result = openForm;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        if(modifyContext || hasDebugLocals())
            return ActionDelegationType.BEFORE_DELEGATE;
        return null;
    }

    @Override
    @IdentityStartLazy
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        boolean lookingForChangeFlow = false;
        boolean lookingForChange = true;
        ImList<ActionMapImplement<?, PropertyInterface>> actions = getActions();
        for(int i = actions.size() - 1; i>= 0; i--) {
            Action<?> listAction = actions.get(i).action;
            
            if(lookingForChangeFlow && (listAction.hasFlow(ChangeFlowType.BREAK) || listAction.hasFlow(ChangeFlowType.RETURN)))
                return false;

            boolean endsWithApply = listAction.endsWithApplyAndNoChangesAfterBreaksBefore();
            if(endsWithApply) {
                lookingForChange = false; // change'и уже не важны, только возможность уйти за пределы APPLY
                lookingForChangeFlow = true;
            }
            
            if(lookingForChange && listAction.hasFlow(ChangeFlowType.FORMCHANGE))
                return false;
        }
        
        return true;
    }
}
