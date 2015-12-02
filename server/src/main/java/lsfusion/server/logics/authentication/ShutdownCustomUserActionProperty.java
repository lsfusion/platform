package lsfusion.server.logics.authentication;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class ShutdownCustomUserActionProperty extends ScriptingActionProperty {
    AuthenticationLogicsModule LM;
    private final ClassPropertyInterface customUserInterface;

    public ShutdownCustomUserActionProperty(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.LM = LM;
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject customUserObject = context.getDataKeyValue(customUserInterface);
        if(customUserObject != null)
            context.getNavigatorsManager().shutdownCustomUser(customUserObject, false);
    }
}