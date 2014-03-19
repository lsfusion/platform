package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class TryActionProperty extends KeepContextActionProperty {

    private final ActionPropertyMapImplement<?, PropertyInterface> tryAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> finallyAction;


    public <I extends PropertyInterface> TryActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, 
                                                           ActionPropertyMapImplement<?, I> tryAction,
                                                           ActionPropertyMapImplement<?, I> finallyAction) {
        super(sID, caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.tryAction = tryAction.map(mapInterfaces);
        this.finallyAction = finallyAction.map(mapInterfaces);

        finalizeInit();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {

        MList<ActionPropertyMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        if(tryAction != null)
            actions.add(tryAction);
        if(finallyAction != null)
            actions.add(finallyAction);
        
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres = 
                ((ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                return value.mapWhereProperty();
            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        if (tryAction != null) {
            result = result.merge(tryAction.property);
        }
        if (finallyAction != null) {
            result = result.merge(finallyAction.property);
        }
        return result;
    }



    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type tryType = tryAction == null ? null : tryAction.property.getSimpleRequestInputType(optimistic);
        Type finallyType = finallyAction == null ? null : finallyAction.property.getSimpleRequestInputType(optimistic);

        if (!optimistic && (tryType == null || finallyType == null)) {
            return null;
        }

        return tryType == null
               ? finallyType
               : finallyType == null
                 ? tryType
                 : tryType.getCompatible(finallyType);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        
        FlowResult result = null;

        if (tryAction != null) {
            try {
                result = tryAction.execute(context);
            } finally {
                if (finallyAction != null) {
                    finallyAction.execute(context);
                }
            }

        }
        return result;
    }
}
