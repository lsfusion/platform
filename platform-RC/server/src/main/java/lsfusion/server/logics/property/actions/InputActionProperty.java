package lsfusion.server.logics.property.actions;

import lsfusion.base.col.ListFact;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.RequestResult;

import java.sql.SQLException;

// можно было бы Input и Form унаследовать от ABSTRACT, но так как наследование зависит от опции INPUT в Form это не совсем корректно
public class InputActionProperty extends SystemExplicitActionProperty {

    private final DataClass dataClass;
    private final LCP targetProp;
            
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
    
}
