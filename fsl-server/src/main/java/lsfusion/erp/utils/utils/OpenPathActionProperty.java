package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Iterator;

public class OpenPathActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;

    public OpenPathActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String source = (String) context.getKeyValue(sourceInterface).getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();

            if (source != null) {
                context.delayUserInteraction(new OpenFileClientAction(IOUtils.toByteArray(new FileInputStream(source)),
                        name != null ? name : FilenameUtils.getBaseName(source), BaseUtils.getFileExtension(source)));
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