package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.stack.ExecutionStackAspect;

import java.sql.SQLException;

public class TryActionProperty extends KeepContextActionProperty {

    private final ActionPropertyMapImplement<?, PropertyInterface> tryAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> catchAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> finallyAction;


    public <I extends PropertyInterface> TryActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                           ActionPropertyMapImplement<?, I> tryAction,
                                                           ActionPropertyMapImplement<?, I> catchAction,
                                                           ActionPropertyMapImplement<?, I> finallyAction) {
        super(caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.tryAction = tryAction.map(mapInterfaces);
        this.catchAction = catchAction == null ? null : catchAction.map(mapInterfaces);
        this.finallyAction = finallyAction == null ? null : finallyAction.map(mapInterfaces);

        finalizeInit();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {

        MList<ActionPropertyMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        actions.add(tryAction);
        if(catchAction != null)
            actions.add(catchAction);
        if(finallyAction != null)
            actions.add(finallyAction);
        
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres = 
                ((ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                return value.mapCalcWhereProperty();
            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        result = result.merge(tryAction.property);

        if (catchAction != null) {
            result = result.merge(catchAction.property);
        }
        if (finallyAction != null) {
            result = result.merge(finallyAction.property);
        }
        return result;
    }



    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type tryType = tryAction.property.getSimpleRequestInputType(optimistic, inRequest);
        Type catchType = catchAction == null ? null : catchAction.property.getSimpleRequestInputType(optimistic, inRequest);
        Type finallyType = finallyAction == null ? null : finallyAction.property.getSimpleRequestInputType(optimistic, inRequest);

        if (!optimistic) {
            if (tryType == null) {
                return null;
            }
            if (catchAction != null && catchType == null) {
                return null;
            }
            if (finallyAction != null && finallyType == null) {
                return null;
            }
        }

        Type type = tryType == null ? catchType : (catchType == null ? tryType : tryType.getCompatible(catchType));
        return type == null ? finallyType : (finallyType == null ? type : type.getCompatible(finallyType));
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        
        FlowResult result;

        try {
            result = tryAction.execute(context);
        } catch(Throwable e) {
            if(catchAction != null) {
                context.getBL().LM.messageCaughtException.change(String.valueOf(e), context);
                context.getBL().LM.javaStackTraceCaughtException.change(String.valueOf(e) + "\n" + ThreadUtils.getJavaStack(e.getStackTrace()), context);
                context.getBL().LM.lsfStackTraceCaughtException.change(ExecutionStackAspect.getStackString(Thread.currentThread(), true, true), context);
                catchAction.execute(context);
            }

            //ignore exception if finallyAction == null
            if (finallyAction == null) {
                ExecutionStackAspect.getExceptionStackString(); // drop exception stack string
                result = FlowResult.FINISH;
            } else {
                throw Throwables.propagate(e);
            }
        } finally {
            if (finallyAction != null) {
                ThreadUtils.setFinallyMode(Thread.currentThread(), true);
                try {
                    finallyAction.execute(context);
                } finally {
                    ThreadUtils.setFinallyMode(Thread.currentThread(), false);
                }
            }
        }

        return result;
    }
}
