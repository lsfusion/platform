package lsfusion.server.language;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.language.linear.LA;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LazyActionProperty extends SystemExplicitAction {
    private final Property sourceProperty;
    private LA evaluatedProperty = null;
       
    public LazyActionProperty(LocalizedString caption, Property sourceProperty) {
        super(caption, new LCP(sourceProperty).getInterfaceClasses(ClassType.drillDownPolicy));
        this.sourceProperty = sourceProperty;       
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
    
    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if(evaluatedProperty == null) {
            evaluatedProperty = context.getBL().LM.addDDAProp(sourceProperty);
        }

        List<ObjectValue> objectValues  = new ArrayList<>();

        for(ClassPropertyInterface entry : interfaces)
            objectValues.add(context.getKeyValue(entry));
        
        evaluatedProperty.execute(context, objectValues.toArray(new ObjectValue[objectValues.size()]));
    }
}
