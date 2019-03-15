package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// можно было бы Input и Form унаследовать от ABSTRACT, но так как наследование зависит от опции INPUT в Form это не совсем корректно
public class InputActionProperty extends SystemExplicitAction {

    private final DataClass dataClass;
    private final LP<?> targetProp;
            
    //  используется только для событий поэтому по идее не надо, так как в событиях user activity быть не может
//    public ImMap<Property, Boolean> aspectChangeExtProps() {
//        return getChangeProps(requestCanceledProperty.property, requestedPropertySet.getLCP(dataClass).property);
//    }

    public InputActionProperty(LocalizedString caption, DataClass dataClass, LP targetProp) {
        super(caption, dataClass);

        this.dataClass = dataClass;
        this.targetProp = targetProp;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue userValue = context.inputUserData(dataClass, context.getSingleKeyValue().getValue());
        context.writeRequested(RequestResult.get(userValue, dataClass, targetProp));
    }
    
    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        if(inRequest)
            return dataClass;
        return null;
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        if(targetProp != null)
            return getChangeProps(targetProp.property);
        // тут по хорошему надо getRequestedValue возвращать но для этого BL нужен
        return MapFact.EMPTY();
    }
}
