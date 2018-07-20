package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
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
        byte[] fileBytes = (byte[]) context.getKeyValue(fileInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        if(fileBytes != null) {
            try {
                String fileString = IOUtils.readStreamToString(new ByteArrayInputStream(BaseUtils.getFile(fileBytes)), charset);
                findProperty("resultString[]").change(fileString, context);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}