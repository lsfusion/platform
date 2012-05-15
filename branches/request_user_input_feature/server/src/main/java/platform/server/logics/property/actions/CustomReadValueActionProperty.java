package platform.server.logics.property.actions;

import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public abstract class CustomReadValueActionProperty extends CustomActionProperty {

    protected CustomReadValueActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected CustomReadValueActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    public void executeCustom(ExecutionContext context) throws SQLException {
        Object userValue = null;
        DataClass readType = getReadType(context);
        if(readType!=null) {
            userValue = context.requestUserData(readType, null);
            if(userValue==null)
                return;
        }
        executeRead(context, userValue);
    }

    protected abstract void executeRead(ExecutionContext context, Object userValue) throws SQLException;
    
    protected abstract DataClass getReadType(ExecutionContext context);
}
