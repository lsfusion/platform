package lsfusion.server.logics.scripted;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LazyActionProperty extends SystemExplicitActionProperty {
    private final CalcProperty sourceProperty;
    private LAP evaluatedProperty = null;
       
    public LazyActionProperty(String caption, CalcProperty sourceProperty) {
        super(caption, new LCP(sourceProperty).getInterfaceClasses());
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

        List<DataObject> dataObjects  = new ArrayList<DataObject>();

        for(ClassPropertyInterface entry : interfaces)
            dataObjects.add(context.getDataKeyValue(entry));
        
        evaluatedProperty.execute(context, dataObjects.toArray(new DataObject[dataObjects.size()]));
    }
}
