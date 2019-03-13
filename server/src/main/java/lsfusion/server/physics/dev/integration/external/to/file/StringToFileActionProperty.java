package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class StringToFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface inputValueInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface extensionInterface;

    public StringToFileActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
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
            if (inputValue != null && extension != null) {
                findProperty("resultFile[]").change(new FileData(new RawFileData(inputValue.getBytes(charset)), extension), context);
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
