package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.AnyValuePropertyHolder;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

// можно было бы Input и Form унаследовать от ABSTRACT, но так как наследование зависит от опции INPUT в Form это не совсем корректно
public class InputActionProperty extends SystemExplicitActionProperty {

    private final DataClass dataClass;    

    private final LCP<?> requestCanceledProperty;
    private final AnyValuePropertyHolder requestedPropertySet;

    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(requestCanceledProperty.property, requestedPropertySet.getLCP(dataClass).property);
    }

    public InputActionProperty(LocalizedString caption, DataClass dataClass, LCP requestCanceledProperty, AnyValuePropertyHolder requestedPropertySet) {
        super(caption, dataClass);

        this.dataClass = dataClass;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedPropertySet = requestedPropertySet;
    }

    public static void writeRequested(ObjectValue chosenValue, Type type, ExecutionContext<?> context, AnyValuePropertyHolder requestedPropertySet, LCP<?> requestCanceledProperty) throws SQLException, SQLHandledException {
        if (chosenValue == null) {
            requestCanceledProperty.change(true, context);
            requestedPropertySet.dropChanges(type, context);
        } else {
            requestCanceledProperty.change((Object)null, context);
            requestedPropertySet.write(type, chosenValue, context);
        }        
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue userValue = context.inputUserData(dataClass, context.getSingleKeyValue().getValue());
        writeRequested(userValue, dataClass, context, requestedPropertySet, requestCanceledProperty);
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        if(inRequest)
            return dataClass;
        return null;
    }
    
}
