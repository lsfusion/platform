package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class StringToFileAction extends InternalAction {
    private final ClassPropertyInterface inputValueInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface extensionInterface;

    public StringToFileAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        inputValueInterface = i.next();
        charsetInterface = i.next();
        extensionInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String inputValue = (String) context.getKeyValue(inputValueInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        String extension = (String) context.getKeyValue(extensionInterface).getValue();

        try {
            FileData fileData = inputValue != null && extension != null ? new FileData(new RawFileData(inputValue, charset), extension) : null;
            findProperty("resultFile[]").change(fileData, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
