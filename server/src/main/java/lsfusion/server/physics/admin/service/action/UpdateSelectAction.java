package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class UpdateSelectAction extends InternalAction {

    public UpdateSelectAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue propertyObject = getParamValue(0, context);
        String select = (String) getParamValue(1, context).getValue();
        Property<?> property = context.getBL().findProperty(((String) context.getBL().reflectionLM.canonicalNameProperty.read(context, propertyObject)).trim()).property;
        property.setSelect(select);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
