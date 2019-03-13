package lsfusion.server.language;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LazyActionProperty extends SystemExplicitActionProperty {
    private final CalcProperty sourceProperty;
    private LAP evaluatedProperty = null;
       
    public LazyActionProperty(LocalizedString caption, CalcProperty sourceProperty) {
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
