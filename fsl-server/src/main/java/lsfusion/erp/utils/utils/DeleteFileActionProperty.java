package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ReadUtils;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface isClientInterface;

    public DeleteFileActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String sourcePath = (String) context.getKeyValue(sourceInterface).getValue();
            boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
            if (sourcePath != null) {

                if (isClient) {
                    Pattern p = Pattern.compile("(?:file:(?://)?)?(.*)");
                    Matcher m = p.matcher(sourcePath);
                    if (m.matches()) {
                        context.delayUserInteraction(new FileClientAction(1, sourcePath.replace("file://", "")));
                    } else {
                        throw Throwables.propagate(new RuntimeException("Unsupported delete source: " + sourcePath + ". Please use format: file://path"));
                    }

                } else {
                    Pattern p = Pattern.compile("(?:(file|ftp|sftp):(?://)?)?(.*)");
                    Matcher m = p.matcher(sourcePath);
                    if (m.matches()) {
                        String type = m.group(1) == null ? "file" : m.group(1).toLowerCase();
                        ReadUtils.deleteFile(type, sourcePath.replace("file://", ""));
                    } else {
                        throw Throwables.propagate(new RuntimeException("Unsupported delete source: " + sourcePath + ". Please use format: file://path or ftp|sftp://username:password;charset@host:port/path"));
                    }
                }
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