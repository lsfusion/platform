package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.List;

public class GetPropertyDependentsAction extends InternalAction {

    public GetPropertyDependentsAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue propertyObject = getParamValue(0, context);
        boolean dependencies = getParam(1, context) != null;
        BusinessLogics BL = context.getBL();
        String propertyCanonicalName = (String) BL.reflectionLM.canonicalNameProperty.read(context, propertyObject);
        List<Property> properties = context.getDbManager().getDependentProperties(context.getSession(), propertyCanonicalName, dependencies);

        for(int i = 0; i < properties.size(); i++) {
            (dependencies ? BL.reflectionLM.propertyDependencies : BL.reflectionLM.propertyDependents).change(BL.reflectionLM.propertyCanonicalName.read(context.getSession(), new DataObject(properties.get(i).getCanonicalName())), context, new DataObject(i));
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}