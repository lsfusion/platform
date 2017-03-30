package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class RequestActionProperty extends KeepContextActionProperty {
    
    private final ActionPropertyMapImplement<?, PropertyInterface> requestAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> doAction;

    public <I extends PropertyInterface> RequestActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> requestAction, ActionPropertyMapImplement<?, I> doAction) {
        super(caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.requestAction = requestAction.map(mapInterfaces);
        this.doAction = doAction.map(mapInterfaces);

        finalizeInit();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {

        MList<ActionPropertyMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        actions.add(requestAction);
        actions.add(doAction);

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
                            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                                return value.mapCalcWhereProperty();
                            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
    }


    public ImSet<ActionProperty> getDependActions() {
        return SetFact.<ActionProperty>toSet(requestAction.property, doAction.property);
    }

    @Override
    public FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;

        boolean isRequestPushed = context.isRequestPushed();
        if (!isRequestPushed)
            result = requestAction.execute(context);

        if (result != FlowResult.FINISH)
            return result;

        boolean isRequestCanceled = context.isRequestCanceled();
        if (!isRequestCanceled) {
            if (isRequestPushed) { // оптимизация
                result = context.popRequest(new SQLCallable<FlowResult>() {
                    public FlowResult call() throws SQLException, SQLHandledException {
                        return doAction.execute(context);
                    }
                });
            } else
                result = doAction.execute(context);
        }

        return result;
    }

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return requestAction.property.getSimpleRequestInputType(optimistic, true);
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return requestAction.property.ignoreReadOnlyPolicy() && doAction.property.ignoreReadOnlyPolicy();
    }

}

