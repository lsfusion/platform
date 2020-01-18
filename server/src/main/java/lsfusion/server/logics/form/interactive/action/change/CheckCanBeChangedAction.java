package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class CheckCanBeChangedAction extends KeepContextAction {

    private final PropertyMapImplement<?, PropertyInterface> changeProp;

    public <I extends PropertyInterface> CheckCanBeChangedAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, PropertyMapImplement<?, I> changeProp) {
        super(caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.changeProp = changeProp.map(mapInterfaces);
        
        finalizeInit();
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return changeProp;
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        return MapFact.singleton(changeProp.property, false);
    }

    @Override
    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<PropertyInterface, DataObject> keys = context.getDataKeys();

        final PropertyValueImplement<?> propertyValues = changeProp.mapValues(keys);
        if(!propertyValues.canBeChanged(context.getModifier())) {
            context.delayUserInteraction(EditNotPerformedClientAction.instance);
            return FlowResult.RETURN;
        }
        return FlowResult.FINISH;
    }
}
