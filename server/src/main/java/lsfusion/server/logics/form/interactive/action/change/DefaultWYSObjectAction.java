package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class DefaultWYSObjectAction<P extends PropertyInterface> extends AbstractDefaultChangeAction<P> {

    private final CustomClass valueClass;

    public DefaultWYSObjectAction(LocalizedString caption, Property<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses, CustomClass valueClass) {
        super(caption, property, listInterfaces, valueClasses.toArray(new ValueClass[valueClasses.size()]));

        this.valueClass = valueClass;
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return ObjectType.idClass;
    }

    @Override
    protected ObjectValue requestValue(ExecutionContext<ClassPropertyInterface> context, ImMap<ClassPropertyInterface, DataObject> keys, PropertyValueImplement<P> propertyValues) throws SQLException, SQLHandledException {
        Object oldValue = implement.read(context, keys);
        ObjectValue changeValue = context.requestUserData(ObjectType.idClass, oldValue, true);
        if(changeValue != null)
            changeValue = context.getSession().getObjectValue(valueClass, (Long)changeValue.getValue());
        return changeValue;
    }
}
