package lsfusion.server.physics.admin.drilldown.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LazyAction extends SystemExplicitAction {
    private final Property sourceProperty;
    private LA evaluatedProperty = null;
       
    public LazyAction(LocalizedString caption, Property sourceProperty) {
        super(caption, new LP(sourceProperty).getInterfaceClasses(ClassType.drillDownPolicy));
        this.sourceProperty = sourceProperty;       
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if(evaluatedProperty == null) {
            evaluatedProperty = context.getBL().LM.addDDAProp(sourceProperty);
        }

        List<ObjectValue> objectValues  = new ArrayList<>();

        for(ClassPropertyInterface entry : getReflectionOrderInterfaces())
            objectValues.add(context.getKeyValue(entry));
        
        evaluatedProperty.execute(context, objectValues.toArray(new ObjectValue[objectValues.size()]));
    }
}
