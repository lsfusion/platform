package platform.server.logics.property.actions;

import platform.interop.form.UserInputResult;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
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
    public void execute(ExecutionContext context) throws SQLException {
        Object userValue = null;
        Type readType = getReadType(context);
        if(readType!=null) {
            UserInputResult userInputResult = context.requestUserInput(readType, null);
            if(userInputResult.isCanceled())
                return;
            userValue = userInputResult.getValue();
        }
        executeRead(context, userValue);
    }

    protected abstract void executeRead(ExecutionContext context, Object userValue) throws SQLException;
    
    protected abstract Type getReadType(ExecutionContext context);
}
