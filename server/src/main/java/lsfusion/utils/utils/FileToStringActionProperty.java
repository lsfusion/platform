package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.FileData;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class FileToStringActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface charsetInterface;

    public FileToStringActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        charsetInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FileData fileData = (FileData) context.getKeyValue(fileInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        try {
            String fileString = fileData != null ? IOUtils.readStreamToString(fileData.getRawFile().getInputStream(), charset) : null;
            findProperty("resultString[]").change(fileString, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}