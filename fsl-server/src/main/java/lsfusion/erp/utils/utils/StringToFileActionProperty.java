package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class StringToFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface inputValueInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface extensionInterface;

    public StringToFileActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        inputValueInterface = i.next();
        charsetInterface = i.next();
        extensionInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String inputValue = (String) context.getKeyValue(inputValueInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        String extension = (String) context.getKeyValue(extensionInterface).getValue();

        try {
            byte[] file = inputValue == null ? new byte[0] : inputValue.getBytes(charset);
            byte[] ext = extension == null ? new byte[0] : extension.getBytes(charset);
            byte[] fileBytes = ext.length == 0 ? BaseUtils.mergeFileWithoutExtension(file) : BaseUtils.mergeFileAndExtension(file, ext);
            if (fileBytes != null) {
                findProperty("resultCustomFile[]").change(fileBytes, context);
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
