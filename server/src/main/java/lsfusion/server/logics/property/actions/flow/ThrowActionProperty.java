package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ThrowActionProperty extends KeepContextActionProperty {

    private final CalcPropertyInterfaceImplement<PropertyInterface> messageProp;

    public <I extends PropertyInterface> ThrowActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> messageProp) {
        super(caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.messageProp = messageProp.map(mapInterfaces);

        finalizeInit();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createUnion(interfaces, ListFact.singleton(messageProp));
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
        messageProp.mapFillDepends(used);
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String message = (String) messageProp.read(context, context.getKeys());
        throw new LSFException(message);
    }
}