package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class FileExistsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface isClientInterface;

    public FileExistsActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        charsetInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String path = (String) context.getKeyValue(pathInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        try {
            context.getSession().dropChanges((DataProperty) findProperty("fileExists[]").property);
            if (path != null) {
                findProperty("fileExists[]").change(FileUtils.checkFileExists(context, path, charset, isClient) ? true : null, context);
            } else {
                throw new RuntimeException("FileExists Error. Path not specified.");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}