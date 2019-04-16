package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class AbstractDefaultChangeAction<P extends PropertyInterface> extends SystemExplicitAction {

    protected final PropertyMapImplement<P, ClassPropertyInterface> implement;

    public AbstractDefaultChangeAction(LocalizedString caption, Property<P> property, ImOrderSet<P> listInterfaces, ValueClass... classes) {
        super(caption, classes);
        this.implement = new PropertyMapImplement<>(property, getMapInterfaces(listInterfaces).reverse());
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(SetAction.hasFlow(implement, type))
            return true;
        return super.hasFlow(type);
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ImMap<ClassPropertyInterface, DataObject> keys = context.getDataKeys();

        final PropertyValueImplement<P> propertyValues = implement.mapValues(keys);
        if(!propertyValues.canBeChanged(context.getModifier())) {
            context.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        ObjectValue changeValue = requestValue(context, keys, propertyValues);

        if (changeValue != null) {
            implement.change(keys, context.getEnv(), changeValue);
        }
    }

    protected abstract ObjectValue requestValue(ExecutionContext<ClassPropertyInterface> context, ImMap<ClassPropertyInterface, DataObject> keys, PropertyValueImplement<P> propertyValues) throws SQLException, SQLHandledException;

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(implement.property);
    }
}
