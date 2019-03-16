package lsfusion.server.physics.dev.integration.external.to.equ.fax;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.equ.fax.client.WriteToFaxClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class WriteToFaxActionProperty extends InternalAction {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface faxNumberInterface;


    public WriteToFaxActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
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
