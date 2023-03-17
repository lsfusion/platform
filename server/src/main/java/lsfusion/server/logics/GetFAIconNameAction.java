package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class GetFAIconNameAction extends InternalAction {
    private final ClassPropertyInterface searchPhraseInterface;

    public GetFAIconNameAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        searchPhraseInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String searchPhrase = (String) context.getDataKeyValue(searchPhraseInterface).getValue();
        try {
            findAction("getBestIcon[STRING]").execute(context, new DataObject(String.join(" | ", AppServerImage.splitCamelCase(searchPhrase))));
            Object bestIconName = findProperty("bestIconName[]").read(context);
            System.out.println(bestIconName);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
