package lsfusion.erp.utils.fax;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class WriteToFaxActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface faxNumberInterface;


    public WriteToFaxActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        faxNumberInterface = i.next();

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String text = (String) context.getDataKeyValue(textInterface).object;
        String faxNumber = (String) context.getDataKeyValue(faxNumberInterface).object;

        String result = (String) context.requestUserInteraction(new WriteToFaxClientAction(text, faxNumber));
        if(result != null)
            context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));

    }
}
