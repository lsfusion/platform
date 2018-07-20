package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class MkdirActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface directoryInterface;

    public MkdirActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        directoryInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String directory = (String) context.getKeyValue(directoryInterface).getValue();
        if (directory != null && !directory.isEmpty()) {
            try {
                FileUtils.mkdir(directory);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw new RuntimeException("Path not specified");
        }
    }
}