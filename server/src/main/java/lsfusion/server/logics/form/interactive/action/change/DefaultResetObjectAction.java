package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class DefaultResetObjectAction<P extends PropertyInterface> extends AbstractDefaultChangeAction<P> {

    public DefaultResetObjectAction(LocalizedString caption, Property<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses) {
        super(caption, property, listInterfaces, valueClasses.toArray(new ValueClass[valueClasses.size()]));
    }

    @Override
    protected ObjectValue requestValue(ExecutionContext<ClassPropertyInterface> context, ImMap<ClassPropertyInterface, DataObject> keys, PropertyValueImplement<P> propertyValues) {
        return NullValue.instance;
    }
}