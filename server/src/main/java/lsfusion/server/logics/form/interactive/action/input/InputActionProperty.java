package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitActionProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.RequestResult;

import java.sql.SQLException;

// можно было бы Input и Form унаследовать от ABSTRACT, но так как наследование зависит от опции INPUT в Form это не совсем корректно
public class InputActionProperty extends SystemExplicitActionProperty {

    private final DataClass dataClass;
    private final LCP<?> targetProp;
            
    //  используется только для событий поэтому по идее не надо, так как в событиях user activity быть не может
//    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
//        return getChangeProps(requestCanceledProperty.property, requestedPropertySet.getLCP(dataClass).property);
//    }

    public InputActionProperty(LocalizedString caption, DataClass dataClass, LCP targetProp) {
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
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        if(targetProp != null)
            return getChangeProps(targetProp.property);
        // тут по хорошему надо getRequestedValue возвращать но для этого BL нужен
        return MapFact.EMPTY();
    }
}
