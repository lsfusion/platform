package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.file.StringWithFiles;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.FileNotFoundException;
import java.sql.SQLException;

// what a string value with one resource between two separators reads when it is sent to a client (FormChanges.convertFileValue),
// written into inlineFilesRead: `resource <its name>`, or `not found`
public class ReadInlineFilesAction extends InternalAction {

    public ReadInlineFilesAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String read;
        try {
            StringWithFiles sent = (StringWithFiles) FormChanges.convertFileValue((String) getParam(0, context), context.getRemoteContext());
            read = "resource " + ((StringWithFiles.Resource) sent.files[0]).name;
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof FileNotFoundException))
                throw e;
            read = "not found";
        }
        try {
            findProperty("inlineFilesRead[]").change(read, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
